
public class Crane {

	public Crane() {

	}

	public void move(int x1, int y1, int x2, int y2, int mode, int[][][] stage, int[][][] tsa) {
		if (mode == 0) {
			tsa[x2][y2][0] = stage[x1][y1][0];
			tsa[x2][y2][1] = stage[x1][y1][1];
			stage[x1][y1][0] = 0;
			stage[x1][y1][1] = 0;
		} else if (mode == 1) {
			stage[x2][y2][0] = tsa[x1][y1][0];
			stage[x2][y2][1] = tsa[x1][y1][1];
			tsa[x1][y1][0] = 0;
			tsa[x1][y1][1] = 0;
		} else if (mode == 2) {
			stage[x2][y2][0] = stage[x1][y1][0];
			stage[x2][y2][1] = stage[x1][y1][1];
			stage[x1][y1][0] = 0;
			stage[x1][y1][1] = 0;
		} else if (mode == 3) {
			tsa[x2][y2][0] = tsa[x1][y1][0];
			tsa[x2][y2][1] = tsa[x1][y1][1];
			tsa[x1][y1][0] = 0;
			tsa[x1][y1][1] = 0;
		}
	}
}
