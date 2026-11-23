package kr.ac.jbnu.se.tetris.game;

import kr.ac.jbnu.se.tetris.save.Settings;

import java.util.EnumMap;
import java.util.Map;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;

/**
 * 효과음. 음원 파일 없이 8비트 비프음을 직접 합성해서 쓴다.
 *
 * 소리 장치가 없는 컴퓨터에서도 게임이 돌아가야 하므로, 실패하면 조용히 꺼진다.
 *
 * 진짜 음원으로 바꾸려면 Sound 에 파일 이름을 달고 load() 에서
 * AudioSystem.getAudioInputStream(resource) 를 쓴다. (wav 만 된다)
 */
public final class Sfx {

	/** 소리 하나의 설계: 시작 주파수 -> 끝 주파수, 길이(ms), 잡음 여부 */
	public enum Sound {
		MOVE(440, 440, 40, false),
		ROTATE(620, 700, 50, false),
		DROP(300, 120, 70, false),
		LOCK(180, 160, 45, false),
		CLEAR(520, 1040, 180, false),
		LEVEL_UP(520, 1560, 260, false),
		ITEM(880, 1320, 90, false),
		COIN(990, 1320, 140, false),
		GAME_OVER(440, 110, 600, false),
		GARBAGE(200, 90, 160, true);

		final int from, to, ms;
		final boolean noise;

		Sound(int from, int to, int ms, boolean noise) {
			this.from = from; this.to = to; this.ms = ms; this.noise = noise;
		}
	}

	private static final float RATE = 22050f;
	private static final AudioFormat FORMAT = new AudioFormat(RATE, 16, 1, true, false);
	private static final Map<Sound, Clip> CLIPS = new EnumMap<>(Sound.class);
	private static boolean broken = false;   // 소리 장치가 없으면 true

	private Sfx() {
	}

	/** 소리 하나를 낸다. 볼륨이 0이거나 장치가 없으면 아무 일도 안 한다. */
	public static void play(Sound sound) {
		if (broken || Settings.sfx <= 0)
			return;
		try {
			Clip clip = CLIPS.computeIfAbsent(sound, Sfx::load);
			if (clip == null)
				return;
			volume(clip, Settings.sfx);
			clip.stop();
			clip.setFramePosition(0);
			clip.start();
		} catch (Exception e) {
			broken = true;   // 한 번 실패하면 다시 시도하지 않는다
		}
	}

	private static Clip load(Sound sound) {
		try {
			byte[] pcm = synth(sound);
			Clip clip = AudioSystem.getClip();
			clip.open(FORMAT, pcm, 0, pcm.length);
			return clip;
		} catch (Exception e) {
			broken = true;
			return null;
		}
	}

	/** 사각파(또는 잡음)를 만들고 뒤로 갈수록 작아지게 한다. */
	private static byte[] synth(Sound sound) {
		int frames = (int) (RATE * sound.ms / 1000);
		byte[] out = new byte[frames * 2];
		double phase = 0;
		for (int i = 0; i < frames; i++) {
			double t = (double) i / frames;
			double freq = sound.from + (sound.to - sound.from) * t;
			phase += freq / RATE;
			double wave = sound.noise ? (Math.random() * 2 - 1) : (phase % 1 < 0.5 ? 1 : -1);
			double fade = 1 - t;                       // 끝으로 갈수록 작아진다
			short v = (short) (wave * fade * 7000);    // 너무 크면 귀가 아프다
			out[i * 2] = (byte) (v & 0xFF);
			out[i * 2 + 1] = (byte) ((v >> 8) & 0xFF);
		}
		return out;
	}

	/** 0~100 설정값을 데시벨로 바꿔 건다. */
	private static void volume(Clip clip, int percent) {
		if (!clip.isControlSupported(FloatControl.Type.MASTER_GAIN))
			return;
		FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
		float db = (float) (20 * Math.log10(Math.max(1, percent) / 100.0));
		gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), db)));
	}

	/** 자가 점검 겸 소리 확인. java kr.ac.jbnu.se.tetris.Sfx 로 전부 들어 본다. */
	public static void main(String[] args) throws Exception {
		for (Sound s : Sound.values()) {
			assert synth(s).length > 0 : s + " 가 빈 소리다";
			System.out.println(s);
			play(s);
			Thread.sleep(500);
		}
		System.out.println(broken ? "소리 장치를 찾지 못했습니다 (게임은 그대로 돌아갑니다)" : "ok");
	}
}
