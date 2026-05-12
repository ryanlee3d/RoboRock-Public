// UNIQUE PATCHED COPY: player death sound support, generated 2026-05-12
package bullet;

import org.joml.Vector3f;
import tage.Camera;
import tage.Engine;
import tage.GameObject;
import tage.audio.AudioResource;
import tage.audio.AudioResourceType;
import tage.audio.IAudioManager;
import tage.audio.Sound;
import tage.audio.SoundType;

public class GameAudio
{
    private static final float SHOTGUN_PUMP_DELAY = 0.35f;

    private IAudioManager audioMgr;
    private Sound healthPickupSound;
    private Sound ammoPickupSound;
    private Sound pistolShotSound;
    private Sound rifleShotSound;
    private Sound plasmaRifleSound;
    private Sound shotgunShotSound;
    private Sound shotgunPumpSound;
    private Sound npcPlasmaSound;
    private float npcPlasmaSoundTimer = 0.0f;
    private final float npcPlasmaSoundCooldown = 0.15f;
    private Sound apeDieSound;
    private Sound alienDieSound;
    private Sound playerDieSound;
    private Sound roarSound;
    private Sound level1Music;
    private Sound level2Music;
    private Sound bossMusic;
    private Sound currentMusic;

    private float shotgunPumpTimer = 0.0f;

    public void initialize(Engine engine)
    {
        audioMgr = engine.getAudioManager();
        if (audioMgr == null)
        {
            System.out.println("Audio manager not available from engine.");
            return;
        }

        AudioResource healthRes = audioMgr.createAudioResource("healthPickup.wav", AudioResourceType.AUDIO_SAMPLE);
        AudioResource ammoRes = audioMgr.createAudioResource("ammoPickup.wav", AudioResourceType.AUDIO_SAMPLE);
        AudioResource pistolRes = audioMgr.createAudioResource("pistol.wav", AudioResourceType.AUDIO_SAMPLE);
        AudioResource rifleRes = audioMgr.createAudioResource("rifle.wav", AudioResourceType.AUDIO_SAMPLE);
        AudioResource plasmaRes = audioMgr.createAudioResource("plasmaRifle.wav", AudioResourceType.AUDIO_SAMPLE);
        AudioResource shotgunRes = audioMgr.createAudioResource("shotGun.wav", AudioResourceType.AUDIO_SAMPLE);
        AudioResource pumpRes = audioMgr.createAudioResource("sgPump.wav", AudioResourceType.AUDIO_SAMPLE);
        AudioResource npcPlasmaRes = audioMgr.createAudioResource("npcPlasma.wav", AudioResourceType.AUDIO_SAMPLE);
        AudioResource apeDieRes = audioMgr.createAudioResource("apeDie.wav", AudioResourceType.AUDIO_SAMPLE);
        AudioResource roarRes = audioMgr.createAudioResource("roar.wav", AudioResourceType.AUDIO_SAMPLE);
        AudioResource alienDieRes = audioMgr.createAudioResource("alienDie.wav", AudioResourceType.AUDIO_SAMPLE);
        AudioResource playerDieRes = audioMgr.createAudioResource("die.wav", AudioResourceType.AUDIO_SAMPLE);
        AudioResource level1MusicRes = audioMgr.createAudioResource("level1.wav", AudioResourceType.AUDIO_STREAM);
        AudioResource level2MusicRes = audioMgr.createAudioResource("level2.wav", AudioResourceType.AUDIO_STREAM);
        AudioResource bossMusicRes = audioMgr.createAudioResource("boss.wav", AudioResourceType.AUDIO_STREAM);

        healthPickupSound = createSound(healthRes, 100, false);
        ammoPickupSound = createSound(ammoRes, 100, false);
        pistolShotSound = createSound(pistolRes, 10, false);
        rifleShotSound = createSound(rifleRes, 100, true);
        plasmaRifleSound = createSound(plasmaRes, 10, false);
        shotgunShotSound = createSound(shotgunRes, 10, false);
        shotgunPumpSound = createSound(pumpRes, 10, false);
        npcPlasmaSound = createSound(npcPlasmaRes, 80, false);
        apeDieSound = createSound(apeDieRes, 80, false);
        alienDieSound = createSound(alienDieRes, 30, false);
        playerDieSound = createSound(playerDieRes, 100, false);
        roarSound = createSound(roarRes, 100, false);
        level1Music = createMusic(level1MusicRes, 100, true);
        level2Music = createMusic(level2MusicRes, 100, true);
        bossMusic = createMusic(bossMusicRes, 100, true);

        configure3DSound(healthPickupSound, 20.0f, 0.5f, 2.0f);
        configure3DSound(ammoPickupSound, 20.0f, 0.5f, 2.0f);
        configure3DSound(npcPlasmaSound, 120.0f, 1.0f, 0.7f);
        configure3DSound(apeDieSound, 35.0f, 1.0f, 2.0f);
        configure3DSound(alienDieSound, 35.0f, 1.0f, 2.0f);
        
    }

    public void releaseAll()
    {
        if (audioMgr == null) return;
        stopMusic();
        stopRifleLoopSound();
        release(healthPickupSound);
        release(ammoPickupSound);
        release(pistolShotSound);
        release(rifleShotSound);
        release(plasmaRifleSound);
        release(shotgunShotSound);
        release(shotgunPumpSound);
        release(npcPlasmaSound);
        release(apeDieSound);
        release(alienDieSound);
        release(playerDieSound);
        release(roarSound);
        release(level1Music);
        release(level2Music);
        release(bossMusic);
    }

    public void setEarParameters(GameObject player, Camera cam)
    {
        if (audioMgr == null || player == null || cam == null) return;

        audioMgr.getEar().setLocation(player.getWorldLocation());

        Vector3f forward = new Vector3f(cam.getN()).mul(1.0f).normalize(); //-1 to flip sound orientation
        audioMgr.getEar().setOrientation(forward, new Vector3f(0.0f, 1.0f, 0.0f));
    }

    public boolean isShotgunPumping()
    {
        return shotgunPumpTimer > 0.0f;
    }

    public void startShotgunPump(float duration)
    {
        shotgunPumpTimer = duration;
    }

    public void updateWeaponAudio(float dt)
    {
        if (npcPlasmaSoundTimer > 0.0f)
        {
            npcPlasmaSoundTimer -= dt;

            if (npcPlasmaSoundTimer < 0.0f)
                npcPlasmaSoundTimer = 0.0f;
        }

        if (shotgunPumpTimer > 0.0f)
        {
            float previous = shotgunPumpTimer;
            shotgunPumpTimer -= dt;

            if (previous > SHOTGUN_PUMP_DELAY && shotgunPumpTimer <= SHOTGUN_PUMP_DELAY)
                play(shotgunPumpSound);

            if (shotgunPumpTimer < 0.0f)
                shotgunPumpTimer = 0.0f;
        }
    }

    public void playHealthPickup(Vector3f location)
    {
        playAt(healthPickupSound, location);
    }

    public void playAmmoPickup(Vector3f location)
    {
        playAt(ammoPickupSound, location);
    }

    public void playPistolShot()
    {
        play(pistolShotSound);
    }

    public void playPlasmaRifle()
    {
        play(plasmaRifleSound);
    }

    public void playShotgunShot()
    {
        play(shotgunShotSound);
    }

    public void playRifleLoopSound()
    {
        if (rifleShotSound != null && !rifleShotSound.getIsPlaying())
            rifleShotSound.play();
    }

    public void stopRifleLoopSound()
    {
        if (rifleShotSound != null && rifleShotSound.getIsPlaying())
            rifleShotSound.stop();
    }

    public void playNpcPlasma(Vector3f location)
    {
        if (npcPlasmaSoundTimer > 0.0f)
            return;

        playAt(npcPlasmaSound, location);
        npcPlasmaSoundTimer = npcPlasmaSoundCooldown;
    }

    public void playApeDie(Vector3f location)
    {
        playAt(apeDieSound, location);
    }

    public void playAlienDie(Vector3f location)
    {
        playAt(alienDieSound, location);
    }

    public void playPlayerDie(Vector3f location)
    {
        if (playerDieSound != null)
        {
            playerDieSound.play();
        }
        else
        {
            System.out.println("Player death sound missing");
        }
    }

    public void playRoar()
    {
        if (roarSound == null) return;

        if (roarSound.getIsPlaying())
            roarSound.stop();

        roarSound.play();
    }

    public void playLevel1Music()
    {
        playMusic(level1Music);
    }

    public void playLevel2Music()
    {
        playMusic(level2Music);
    }

    public void playBossMusic()
    {
        playMusic(bossMusic);
    }

    public void stopMusic()
    {
        if (currentMusic != null && currentMusic.getIsPlaying())
            currentMusic.stop();

        currentMusic = null;
    }

    private void playMusic(Sound music)
    {
        if (music == null)
        {
            System.out.println("Music failed: Sound is null");
            return;
        }

        if (currentMusic == music && music.getIsPlaying())
            return;

        stopMusic();

        currentMusic = music;
        currentMusic.play();
    }

    private Sound createSound(AudioResource resource, int volume, boolean loop)
    {
        return createSound(resource, SoundType.SOUND_EFFECT, volume, loop);
    }

    private Sound createMusic(AudioResource resource, int volume, boolean loop)
    {
        return createSound(resource, SoundType.SOUND_MUSIC, volume, loop);
    }

    private Sound createSound(AudioResource resource, SoundType type, int volume, boolean loop)
    {
        if (audioMgr == null || resource == null)
            return null;

        Sound sound = new Sound(resource, type, volume, loop);
        sound.initialize(audioMgr);
        return sound;
    }

    private void release(Sound sound)
    {
        if (sound != null)
            sound.release(audioMgr);
    }

    private void play(Sound sound)
    {
        if (sound != null)
            sound.play();
    }

    private void playAt(Sound sound, Vector3f location)
    {
        if (sound == null || location == null) return;

        sound.setLocation(location);

        if (sound.getIsPlaying())
            sound.stop();

        sound.play();
    }

    private void configure3DSound(Sound sound, float maxDist, float minDist, float rolloff)
    {
        if (sound == null) return;

        sound.setMaxDistance(maxDist);
        sound.setMinDistance(minDist);
        sound.setRollOff(rolloff);
    }
}
