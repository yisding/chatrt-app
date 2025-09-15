@file:Suppress("PropertyName")

package ai.chatrt.app.platform

import ai.chatrt.app.models.*
import android.annotation.SuppressLint
import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import android.media.AudioManager as SystemAudioManager
import android.media.AudioManager.OnAudioFocusChangeListener as SystemOnAudioFocusChangeListener

/**
 * Android implementation of AudioManager for audio routing, device management, and focus handling
 * Requirements: 1.5, 2.3, 5.3
 */
class AndroidAudioManager(
    private val context: Context,
) : AudioManager {
    private val systemAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as SystemAudioManager
    private val _audioDeviceChanges = MutableStateFlow<AudioDevice?>(null)
    private val _currentAudioDevice = MutableStateFlow<AudioDevice?>(null)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var audioFocusRequest: AudioFocusRequest? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothHeadset: BluetoothHeadset? = null
    private var audioDeviceReceiver: BroadcastReceiver? = null
    private var isInitialized = false

    private val audioFocusChangeListener =
        SystemOnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                SystemAudioManager.AUDIOFOCUS_GAIN -> {
                    // Resume audio
                    systemAudioManager.mode = SystemAudioManager.MODE_IN_COMMUNICATION
                }
                SystemAudioManager.AUDIOFOCUS_LOSS -> {
                    // Permanently lost focus - stop audio
                    releaseAudioFocusInternal()
                }
                SystemAudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    // Temporarily lost focus - pause audio
                }
                SystemAudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    // Lower volume but continue playing
                }
            }
        }

    private val bluetoothProfileListener =
        object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(
                profile: Int,
                proxy: BluetoothProfile,
            ) {
                if (profile == BluetoothProfile.HEADSET) {
                    bluetoothHeadset = proxy as BluetoothHeadset
                    updateAvailableAudioDevices()
                }
            }

            override fun onServiceDisconnected(profile: Int) {
                if (profile == BluetoothProfile.HEADSET) {
                    bluetoothHeadset = null
                    updateAvailableAudioDevices()
                }
            }
        }

    override suspend fun initialize() {
        if (isInitialized) return

        // Initialize Bluetooth adapter
        val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothAdapter?.getProfileProxy(context, bluetoothProfileListener, BluetoothProfile.HEADSET)

        // Register for audio device changes
        registerAudioDeviceReceiver()

        // Set initial audio device
        updateAvailableAudioDevices()

        isInitialized = true
    }

    override suspend fun setupAudioRouting() {
        // Configure audio routing for WebRTC calls
        systemAudioManager.mode = SystemAudioManager.MODE_IN_COMMUNICATION
        systemAudioManager.isSpeakerphoneOn = false

        // Set default audio device based on availability
        val availableDevices = getAvailableAudioDevices()
        val defaultDevice =
            availableDevices.firstOrNull { it.isDefault }
                ?: availableDevices.firstOrNull()

        defaultDevice?.let { setAudioDevice(it) }
    }

    override suspend fun setAudioMode(mode: AudioMode) {
        val androidMode =
            when (mode) {
                AudioMode.NORMAL -> SystemAudioManager.MODE_NORMAL
                AudioMode.CALL -> SystemAudioManager.MODE_IN_CALL
                AudioMode.COMMUNICATION -> SystemAudioManager.MODE_IN_COMMUNICATION
                AudioMode.RINGTONE -> SystemAudioManager.MODE_RINGTONE
            }
        systemAudioManager.mode = androidMode
    }

    override suspend fun requestAudioFocus(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes =
                AudioAttributes
                    .Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()

            audioFocusRequest =
                AudioFocusRequest
                    .Builder(SystemAudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(audioAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(audioFocusChangeListener)
                    .build()

            val result = systemAudioManager.requestAudioFocus(audioFocusRequest!!)
            result == SystemAudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            val result =
                systemAudioManager.requestAudioFocus(
                    audioFocusChangeListener,
                    SystemAudioManager.STREAM_VOICE_CALL,
                    SystemAudioManager.AUDIOFOCUS_GAIN,
                )
            result == SystemAudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }

    override suspend fun releaseAudioFocus() {
        releaseAudioFocusInternal()
    }

    private fun releaseAudioFocusInternal() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { request ->
                systemAudioManager.abandonAudioFocusRequest(request)
            }
        } else {
            @Suppress("DEPRECATION")
            systemAudioManager.abandonAudioFocus(audioFocusChangeListener)
        }
        audioFocusRequest = null
    }

    override suspend fun handleHeadsetConnection(connected: Boolean) {
        if (connected) {
            // Switch to wired headset if available
            val headsetDevice =
                getAvailableAudioDevices()
                    .firstOrNull { it.type == AudioDeviceType.WIRED_HEADSET }
            headsetDevice?.let { setAudioDevice(it) }
        } else {
            // Switch back to speaker or earpiece
            val fallbackDevice =
                getAvailableAudioDevices()
                    .firstOrNull { it.type == AudioDeviceType.SPEAKER }
                    ?: getAvailableAudioDevices().firstOrNull()
            fallbackDevice?.let { setAudioDevice(it) }
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun getAvailableAudioDevices(): List<AudioDevice> {
        val devices = mutableListOf<AudioDevice>()

        // Always available devices
        devices.add(AudioDevice("speaker", "Speaker", AudioDeviceType.SPEAKER, false))
        devices.add(AudioDevice("earpiece", "Earpiece", AudioDeviceType.EARPIECE, true))

        // Check for wired headset
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as SystemAudioManager
        if (audioManager.isWiredHeadsetOn) {
            devices.add(AudioDevice("wired_headset", "Wired Headset", AudioDeviceType.WIRED_HEADSET, false))
        }

        // Check for Bluetooth headset
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            bluetoothHeadset?.let { headset ->
                try {
                    val connectedDevices = headset.connectedDevices
                    connectedDevices.forEach { device ->
                        devices.add(
                            AudioDevice(
                                id = device.address,
                                name = device.name ?: "Bluetooth Headset",
                                type = AudioDeviceType.BLUETOOTH_HEADSET,
                                isDefault = false,
                            ),
                        )
                    }
                } catch (_: SecurityException) {
                    // Permission might be revoked at runtime; ignore bluetooth devices
                }
            }
        }

        return devices
    }

    override suspend fun setAudioDevice(device: AudioDevice) {
        when (device.type) {
            AudioDeviceType.SPEAKER -> {
                systemAudioManager.isSpeakerphoneOn = true
                systemAudioManager.stopBluetoothSco()
            }
            AudioDeviceType.EARPIECE, AudioDeviceType.WIRED_HEADSET, AudioDeviceType.WIRED_HEADPHONES -> {
                systemAudioManager.isSpeakerphoneOn = false
                systemAudioManager.stopBluetoothSco()
            }
            AudioDeviceType.BLUETOOTH_HEADSET -> {
                systemAudioManager.isSpeakerphoneOn = false
                systemAudioManager.startBluetoothSco()
            }
            AudioDeviceType.USB_HEADSET -> {
                systemAudioManager.isSpeakerphoneOn = false
                systemAudioManager.stopBluetoothSco()
            }
            AudioDeviceType.UNKNOWN -> {
                // Use default routing
            }
        }

        _currentAudioDevice.value = device
        _audioDeviceChanges.value = device
    }

    override suspend fun getCurrentAudioDevice(): AudioDevice? = _currentAudioDevice.value

    override fun observeAudioDeviceChanges(): Flow<AudioDevice> =
        _audioDeviceChanges
            .asStateFlow()
            .filterNotNull()

    override suspend fun setAudioQuality(quality: AudioQuality) {
        // Configure audio parameters based on quality
        when (quality) {
            AudioQuality.LOW -> {
                // Lower sample rate, mono
            }
            AudioQuality.MEDIUM -> {
                // Standard sample rate, stereo
            }
            AudioQuality.HIGH -> {
                // High sample rate, stereo, enhanced processing
            }
        }
    }

    override suspend fun setNoiseSuppression(enabled: Boolean) {
        // Configure noise suppression (would be handled by WebRTC)
        // This is a placeholder for WebRTC integration
    }

    override suspend fun setEchoCancellation(enabled: Boolean) {
        // Configure echo cancellation (would be handled by WebRTC)
        // This is a placeholder for WebRTC integration
    }

    override suspend fun cleanup() {
        releaseAudioFocus()
        unregisterAudioDeviceReceiver()

        bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HEADSET, bluetoothHeadset)
        bluetoothHeadset = null

        systemAudioManager.mode = SystemAudioManager.MODE_NORMAL
        isInitialized = false
    }

    private fun registerAudioDeviceReceiver() {
        audioDeviceReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context?,
                    intent: Intent?,
                ) {
                    scope.launch {
                        when (intent?.action) {
                            SystemAudioManager.ACTION_HEADSET_PLUG -> {
                                val state = intent.getIntExtra("state", -1)
                                val name = intent.getStringExtra("name") ?: "Wired Headset"
                                val microphone = intent.getIntExtra("microphone", -1) == 1

                                handleHeadsetConnectionDetailed(state == 1, name, microphone)
                            }
                            BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                                val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)
                                val device =
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        intent.getParcelableExtra(
                                            android.bluetooth.BluetoothDevice.EXTRA_DEVICE,
                                            android.bluetooth.BluetoothDevice::class.java,
                                        )
                                    } else {
                                        @Suppress("DEPRECATION")
                                        intent.getParcelableExtra(android.bluetooth.BluetoothDevice.EXTRA_DEVICE)
                                    }

                                when (state) {
                                    BluetoothProfile.STATE_CONNECTED -> {
                                        val appCtx = this@AndroidAudioManager.context
                                        if (ContextCompat.checkSelfPermission(appCtx, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                                            handleBluetoothDeviceConnected(device)
                                        }
                                    }
                                    BluetoothProfile.STATE_DISCONNECTED -> {
                                        val appCtx = this@AndroidAudioManager.context
                                        if (ContextCompat.checkSelfPermission(appCtx, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                                            handleBluetoothDeviceDisconnected(device)
                                        }
                                    }
                                }
                            }
                            SystemAudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                                // Handle audio becoming noisy (headphones unplugged during playback)
                                handleAudioBecomingNoisy()
                            }
                            SystemAudioManager.ACTION_SCO_AUDIO_STATE_UPDATED -> {
                                val state = intent.getIntExtra(SystemAudioManager.EXTRA_SCO_AUDIO_STATE, -1)
                                handleBluetoothScoStateChanged(state)
                            }
                        }
                    }
                }
            }

        val filter =
            IntentFilter().apply {
                addAction(SystemAudioManager.ACTION_HEADSET_PLUG)
                addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
                addAction(SystemAudioManager.ACTION_AUDIO_BECOMING_NOISY)
                addAction(SystemAudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
            }
        context.registerReceiver(audioDeviceReceiver, filter)
    }

    private fun unregisterAudioDeviceReceiver() {
        audioDeviceReceiver?.let { receiver ->
            try {
                context.unregisterReceiver(receiver)
            } catch (e: IllegalArgumentException) {
                // Receiver was not registered
            }
        }
        audioDeviceReceiver = null
    }

    private fun updateAvailableAudioDevices() {
        scope.launch {
            val devices = getAvailableAudioDevices()
            val currentDevice = getCurrentAudioDevice()

            // If current device is no longer available, switch to default
            if (currentDevice != null && !devices.contains(currentDevice)) {
                val defaultDevice =
                    devices.firstOrNull { it.isDefault }
                        ?: devices.firstOrNull()
                defaultDevice?.let { setAudioDevice(it) }
            }
        }
    }

    /**
     * Handle detailed headset connection with device information
     * Requirement: 5.3 - Headphone connection/disconnection detection with audio routing
     */
    private suspend fun handleHeadsetConnectionDetailed(
        connected: Boolean,
        name: String,
        hasMicrophone: Boolean,
    ) {
        if (connected) {
            val deviceType = if (hasMicrophone) AudioDeviceType.WIRED_HEADSET else AudioDeviceType.WIRED_HEADPHONES
            val device = AudioDevice("wired_headset", name, deviceType, false)

            // Automatically switch to the connected headset
            setAudioDevice(device)

            // Notify about the connection
            _audioDeviceChanges.value = device
        } else {
            // Switch back to default device (speaker or earpiece)
            val fallbackDevice =
                getAvailableAudioDevices()
                    .firstOrNull { it.type == AudioDeviceType.EARPIECE }
                    ?: getAvailableAudioDevices().firstOrNull { it.type == AudioDeviceType.SPEAKER }

            fallbackDevice?.let {
                setAudioDevice(it)
                _audioDeviceChanges.value = it
            }
        }

        updateAvailableAudioDevices()
    }

    /**
     * Handle Bluetooth device connection
     * Requirement: 5.3 - Bluetooth headphone connection with audio routing
     */
    @SuppressLint("MissingPermission")
    private suspend fun handleBluetoothDeviceConnected(device: android.bluetooth.BluetoothDevice?) {
        if (ContextCompat.checkSelfPermission(this@AndroidAudioManager.context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        device?.let { btDevice ->
            val audioDevice =
                AudioDevice(
                    id = btDevice.address,
                    name = btDevice.name ?: "Bluetooth Headset",
                    type = AudioDeviceType.BLUETOOTH_HEADSET,
                    isDefault = false,
                )

            // Automatically switch to Bluetooth device
            setAudioDevice(audioDevice)
            _audioDeviceChanges.value = audioDevice
        }

        updateAvailableAudioDevices()
    }

    /**
     * Handle Bluetooth device disconnection
     * Requirement: 5.3 - Bluetooth headphone disconnection with audio routing
     */
    private suspend fun handleBluetoothDeviceDisconnected(device: android.bluetooth.BluetoothDevice?) {
        // Switch back to wired headset if available, otherwise speaker/earpiece
        val fallbackDevice =
            getAvailableAudioDevices()
                .firstOrNull { it.type == AudioDeviceType.WIRED_HEADSET }
                ?: getAvailableAudioDevices().firstOrNull { it.type == AudioDeviceType.EARPIECE }
                ?: getAvailableAudioDevices().firstOrNull { it.type == AudioDeviceType.SPEAKER }

        fallbackDevice?.let {
            setAudioDevice(it)
            _audioDeviceChanges.value = it
        }

        updateAvailableAudioDevices()
    }

    /**
     * Handle audio becoming noisy (e.g., headphones unplugged during call)
     * Requirement: 5.3 - Handle device state changes with appropriate UI feedback
     */
    private suspend fun handleAudioBecomingNoisy() {
        // Switch to earpiece to avoid disturbing others
        val earpiece =
            getAvailableAudioDevices()
                .firstOrNull { it.type == AudioDeviceType.EARPIECE }

        earpiece?.let {
            setAudioDevice(it)
            _audioDeviceChanges.value = it
        }
    }

    /**
     * Handle Bluetooth SCO state changes
     */
    private suspend fun handleBluetoothScoStateChanged(state: Int) {
        when (state) {
            SystemAudioManager.SCO_AUDIO_STATE_CONNECTED -> {
                // Bluetooth SCO is now connected and ready for audio
                val bluetoothDevice =
                    getAvailableAudioDevices()
                        .firstOrNull { it.type == AudioDeviceType.BLUETOOTH_HEADSET }
                bluetoothDevice?.let { _audioDeviceChanges.value = it }
            }
            SystemAudioManager.SCO_AUDIO_STATE_DISCONNECTED -> {
                // Bluetooth SCO disconnected, switch to fallback
                val fallbackDevice =
                    getAvailableAudioDevices()
                        .firstOrNull { it.type == AudioDeviceType.WIRED_HEADSET }
                        ?: getAvailableAudioDevices().firstOrNull { it.type == AudioDeviceType.EARPIECE }

                fallbackDevice?.let {
                    setAudioDevice(it)
                    _audioDeviceChanges.value = it
                }
            }
        }
    }

    /**
     * Get detailed audio device information
     */
    suspend fun getAudioDeviceInfo(): AudioDeviceInfo {
        val currentDevice = getCurrentAudioDevice()
        val availableDevices = getAvailableAudioDevices()

        return AudioDeviceInfo(
            currentDevice = currentDevice,
            availableDevices = availableDevices,
            isBluetoothScoOn = systemAudioManager.isBluetoothScoOn,
            isSpeakerphoneOn = systemAudioManager.isSpeakerphoneOn,
            isWiredHeadsetOn = systemAudioManager.isWiredHeadsetOn,
            audioMode = systemAudioManager.mode,
        )
    }

    /**
     * Handle phone call interruption - pause ChatRT audio
     * Requirement: 5.2
     */
    suspend fun handlePhoneCallInterruption() {
        releaseAudioFocus()
        systemAudioManager.mode = SystemAudioManager.MODE_NORMAL
    }

    /**
     * Resume after phone call ends
     * Requirement: 5.2
     */
    suspend fun resumeAfterPhoneCall() {
        requestAudioFocus()
        setupAudioRouting()
    }

    /**
     * Optimize audio for battery saving
     */
    suspend fun optimizeForBattery() {
        // Reduce audio quality for battery saving
        setAudioQuality(AudioQuality.LOW)
    }
}

/**
 * Factory function for creating Android audio manager
 */
actual fun createAudioManager(): AudioManager = throw IllegalStateException("Android AudioManager requires Context. Use AndroidAudioManager(context) directly.")
