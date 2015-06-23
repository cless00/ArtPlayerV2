package darack.com.player;

import java.util.ArrayList;

import android.util.Log;
import darack.com.player.soundfile.CheapSoundFile;

public class AISeeker {

	public static final int MSG_PLAY = 100;
	public static final int MSG_PAUSE = 101;
	public static final int MSG_FINDED = 102;

	private static final String TAG = "TEST_DEBUG";
	private boolean possible; // 공백 검사 여부
	private int space; // 공백 검출시 공백 인정 조건
	private int testCount; // 테스트할 횟수
	private int condition; // 공백 검출 조건 1-128사이

	public AISeeker() {
		this.possible = true;
		this.space = 4000;
		this.testCount = 12;
		this.condition = 5;
	}

	public boolean isPossible() {
		return possible;
	}

	public void setPossible(boolean possible) {
		this.possible = possible;
	}

	public int getCondition() {
		return condition;
	}

	public void setCondition(int condition) {
		this.condition = condition;
	}

	public int getTestCount() {
		return this.testCount;
	}

	public void setTestCount(int testCount) {
		this.testCount = testCount;
	}

	public int getSpace() {
		return space;
	}

	public void setSpace(int space) {
		this.space = space;
	}

	public ArrayList<Integer> getQuestionPos(CheapSoundFile soundFile) {
		ArrayList<Integer> posList = new ArrayList<Integer>();

		int numFrames = soundFile.getNumFrames();
		int[] frameGains = soundFile.getFrameGains();
		double[] smoothedGains = new double[numFrames];

		if (numFrames == 1) {
			smoothedGains[0] = frameGains[0];
		} else if (numFrames == 2) {
			smoothedGains[0] = frameGains[0];
			smoothedGains[1] = frameGains[1];
		} else if (numFrames > 2) {
			smoothedGains[0] = ((frameGains[0] / 2.0) + (frameGains[1] / 2.0));
			for (int i = 1; i < numFrames - 1; i++) {
				smoothedGains[i] = ((frameGains[i - 1] / 3.0)
						+ (frameGains[i] / 3.0) + (frameGains[i + 1] / 3.0));
			}
			smoothedGains[numFrames - 1] = ((frameGains[numFrames - 2] / 2.0) + (frameGains[numFrames - 1] / 2.0));
		}

		// Make sure the range is no more than 0 - 255
		double maxGain = 1.0;
		for (int i = 0; i < numFrames; i++) {
			if (smoothedGains[i] > maxGain) {
				maxGain = smoothedGains[i];
			}
		}
		double scaleFactor = 1.0;
		if (maxGain > 255.0) {
			scaleFactor = 255 / maxGain;
		}

		// Build histogram of 256 bins and figure out the new scaled max
		maxGain = 0;
		int gainHist[] = new int[256];
		for (int i = 0; i < numFrames; i++) {
			int smoothedGain = (int) (smoothedGains[i] * scaleFactor);
			if (smoothedGain < 0)
				smoothedGain = 0;
			if (smoothedGain > 255)
				smoothedGain = 255;

			if (smoothedGain > maxGain)
				maxGain = smoothedGain;

			gainHist[smoothedGain]++;
		}

		// Re-calibrate the min to be 5%
		double minGain = 0;
		int sum = 0;
		while (minGain < 255 && sum < numFrames / 20) {
			sum += gainHist[(int) minGain];
			minGain++;
		}

		// Re-calibrate the max to be 99%
		sum = 0;
		while (maxGain > 2 && sum < numFrames / 100) {
			sum += gainHist[(int) maxGain];
			maxGain--;
		}

		// Compute the heights
		double[] heights = new double[numFrames];
		double range = maxGain - minGain;
		for (int i = 0; i < numFrames; i++) {
			double value = (smoothedGains[i] * scaleFactor - minGain) / range;
			if (value < 0.0)
				value = 0.0;
			if (value > 1.0)
				value = 1.0;
			heights[i] = value * value;
		}

		// // 디버킹 출력
		// for(int i = 0; i < numFrames; i++){
		// Log.d(TAG, "heights[" + i + "] * 128 =" + (heights[i] * 128));
		// }

		// 공백찾기
		int repeat = 0;
		int framePerSec = (int) (1.0 * soundFile.getSampleRate()
				/ soundFile.getSamplesPerFrame() + 0.5);
		int frame = (int) (1.0 * framePerSec * space / testCount / 1000);

		Log.d(TAG, "framePerSec : " + framePerSec);
		Log.d(TAG, "frame : " + frame);

		posList.add(0);

		for (int i = 0; i < numFrames; i += frame) {
			// milisecond로 변환
			int msec = (int) (1.0 * i * soundFile.getSamplesPerFrame()
					/ soundFile.getSampleRate() * 1000.0);

			if (heights[i] * 128 < this.condition) {
				repeat++;
			} else {
				repeat = 0;
			}

			if (repeat >= testCount) {
				repeat = 0;
				posList.add(msec);
				Log.d(TAG, "blank msec :" + msec);
			}
		}

		// Log.d(TAG, "numFrames : " + numFrames);
		// Log.d(TAG, "getSampleRate() : " + soundFile.getSampleRate());
		// Log.d(TAG, "getSamplesPerFrame() : " +
		// soundFile.getSamplesPerFrame());
		// Log.d(TAG, "seconds : " + (numFrames * soundFile.getSamplesPerFrame()
		// / soundFile.getSampleRate()));

		return posList;
	}

}
