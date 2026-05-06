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
    private Sound apePlasmaSound;

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
        AudioResource apePlasmaRes = audioMgr.createAudioResource("apePlasma.wav", AudioResourceType.AUDIO_SAMPLE);

        healthPickupSound = createSound(healthRes, 75, false);
        ammoPickupSound = createSound(ammoRes, 75, false);
        pistolShotSound = createSound(pistolRes, 75, false);
        rifleShotSound = createSound(rifleRes, 75, true);
        plasmaRifleSound = createSound(plasmaRes, 75, false);
        shotgunShotSound = createSound(shotgunRes, 75, false);
        shotgunPumpSound = createSound(pumpRes, 75, false);
        apePlasmaSound = createSound(apePlasmaRes, 100, false);

        if (apePlasmaSound != null)
        {
            apePlasmaSound.setMaxDistance(35.0f);
            apePlasmaSound.setMinDistance(1.0f);
            apePlasmaSound.setRollOff(2.0f);
        }
    }

    public void releaseAll()
    {
        if (audioMgr == null) return;

        release(healthPickupSound);
        release(ammoPickupSound);
        release(pistolShotSound);
        release(rifleShotSound);
        release(plasmaRifleSound);
        release(shotgunShotSound);
        release(shotgunPumpSound);
        release(apePlasmaSound);
    }

    public void setEarParameters(GameObject player, Camera cam)
    {
        if (audioMgr == null || player == null || cam == null) return;

        audioMgr.getEar().setLocation(player.getWorldLocation());

        Vector3f forward = new Vector3f(cam.getN()).mul(-1.0f).normalize();
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

    public void playHealthPickup()
    {
        play(healthPickupSound);
    }

    public void playAmmoPickup()
    {
        play(ammoPickupSound);
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

    public void playApePlasma(Vector3f location)
    {
        if (apePlasmaSound == null) return;

        apePlasmaSound.setLocation(location);
        apePlasmaSound.play();
    }

    private Sound createSound(AudioResource resource, int volume, boolean loop)
    {
        if (audioMgr == null || resource == null) return null;

        Sound sound = new Sound(resource, SoundType.SOUND_EFFECT, volume, loop);
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
}
