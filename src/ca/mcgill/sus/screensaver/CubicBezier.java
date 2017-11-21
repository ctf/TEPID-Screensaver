package ca.mcgill.sus.screensaver;

/**translated from the js: https://github.com/arian/cubic-bezier**/
public abstract class CubicBezier {
	
	public abstract double calc(double t);
	
	private CubicBezier() {
	}

	public static CubicBezier create(final double x1, final double y1, final double x2, final double y2, final double epsilon) {
		return new CubicBezier() {
			public double calc(double t) {
				double x = t, x2, d2, t2 = x;

				// First try a few iterations of Newton's method -- normally very fast.
				for (int i = 0; i < 8; i++){
					x2 = curveX(t2) - x;
					if (Math.abs(x2) < epsilon) return curveY(t2);
					d2 = derivativeCurveX(t2);
					if (Math.abs(d2) < 1e-6) break;
					t2 = t2 - x2 / d2;
				}

				double t0 = 0, t1 = 1;
				t2 = x;

				if (t2 < t0) return curveY(t0);
				if (t2 > t1) return curveY(t1);

				// Fallback to the bisection method for reliability.
				while (t0 < t1){
					x2 = curveX(t2);
					if (Math.abs(x2 - x) < epsilon) return curveY(t2);
					if (x > x2) t0 = t2;
					else t1 = t2;
					t2 = (t1 - t0) * .5 + t0;
				}

				// Failure
				return curveY(t2);
			}
			
			private double curveX(double t){
				double v = 1 - t;
				return 3 * v * v * t * x1 + 3 * v * t * t * x2 + t * t * t;
			}

			private double curveY(double t){
				double v = 1 - t;
				return 3 * v * v * t * y1 + 3 * v * t * t * y2 + t * t * t;
			}

			private double derivativeCurveX(double t){
				double v = 1 - t;
				return 3 * (2 * (t - 1) * t + v * v) * x1 + 3 * (- t * t * t + 2 * v * t) * x2;
			}
		};

	}
	


}
