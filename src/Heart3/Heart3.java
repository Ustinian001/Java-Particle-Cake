package Heart3;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.Timer;

public class Heart3 extends JPanel implements ActionListener {

	private static final int W = 1100;
	private static final int H = 900;

	// 相机：照搬参考程序
	private static final double[] CPOS = { 8.5, -9.3, 6.5 };
	private static final double[] CTGT = { 0.0, 0.0, 1.7 };
	private static final double[] CUP = { 0.0, 0.0, 1.0 };
	private static final double CZOOM = 0.85;
	private static final double FOCAL = 2200.0;
	private static final double YAW = Math.toDegrees(Math.atan2(CPOS[0], -CPOS[1]));

	// 五层蛋糕：半径、高度（自下而上）
	private static final double[][] LAYERS = { { 1.90, 0.85 }, { 1.62, 0.78 }, { 1.36, 0.72 }, { 1.12, 0.66 },
			{ 0.88, 0.60 } };
	private static final int[] LAYER_COL = { 0xFFB3C6, 0xFFE7A8, 0xA8E6CF, 0xA9C9FF, 0xD9BFFF };

	private static final int CD0 = 3;
	private static final int SPN = 12;
	private static final int CDEND = CD0 * SPN;
	private static final int HBPOP = CDEND + 10;
	private static final int HBHOLD = CDEND + 18;
	private static final int HBFADE = CDEND + 32;
	private static final int CAKEF = CDEND + 36;
	private static final int BLIFE = 110;
	private static final double RSPD = 0.4;

	private static final int[] RIBBON_COL = { 0xFF1744, 0xFF6D00, 0xFFD600, 0x76FF03, 0x00E5FF, 0x2979FF, 0xAA00FF,
			0xF50057, 0x00E676, 0xFF9100, 0xFF4081, 0x18FFFF, 0xFFFF00, 0xFF6E40, 0xB388FF, 0x69F0AE };
	private static final int[] CONFETTI_COL = { 0xFF6B9D, 0xFFD93D, 0x6BCBFF, 0xA0E76E, 0xC78CFF, 0xFF9A6B, 0xFF1744,
			0x00E5FF };

	private final String ageStr;
	private int frameCount = 0;
	private double rot = 0;
	private double cakeTop = 0;
	private double flameBase = 0;

	private final ArrayList<P> cake = new ArrayList<>();
	private final ArrayList<P> digits = new ArrayList<>();
	private final ArrayList<P> flameOuter = new ArrayList<>();
	private final ArrayList<P> flameInner = new ArrayList<>();
	private final ArrayList<P> confetti = new ArrayList<>();
	private int starN;
	private double[] starX, starY, starR, starP;
	private int[] starC;
	private int bokN;
	private double[] bokX, bokY, bokR;
	private int[] bokC;
	private final ArrayList<Ribbon> ribbons = new ArrayList<>();
	private boolean bstart = false;

	private final Random rnd = new Random(20240601L);

	private BufferedImage buf;
	private int[] pixels;
	private int[] px, py, pc, ps, nxt;
	private final int[] binHead = new int[256];
	private final int[] tmp = new int[2];

	private static class P {
		double x, y, z;
		int col;
		int size;

		P(double x, double y, double z, int col, int size) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.col = col;
			this.size = size;
		}
	}

	private static class Ribbon {
		double x, y, z, vx, vy, vz, phase;
		int col;
		boolean alive;
		int birth;
	}

	public Heart3(String age) {
		this.ageStr = age;
		setBackground(new Color(0x0B1026));
		buildScene();
		for (int i = 0; i < 520; i++) {
			Ribbon r = new Ribbon();
			r.x = 0;
			r.y = 0;
			r.z = 2.4;
			r.phase = rnd.nextDouble() * Math.PI * 2;
			r.col = RIBBON_COL[i % RIBBON_COL.length];
			r.alive = false;
			ribbons.add(r);
		}
		Timer timer = new Timer(25, this);
		timer.start();
	}

	private void buildScene() {
		for (int i = 0; i < 9000; i++) {
			double th = rnd.nextDouble() * Math.PI * 2;
			double rr = Math.sqrt(rnd.nextDouble()) * 2.35;
			int pcol = (i & 3) == 0 ? 0xD9CCEA : 0xE7DCF2;
			cake.add(new P(rr * Math.cos(th), rr * Math.sin(th), -0.05 + rnd.nextGaussian() * 0.006, pcol, 2));
		}

		double z0 = 0;
		for (int k = 0; k < LAYERS.length; k++) {
			double r = LAYERS[k][0];
			double h = LAYERS[k][1];
			int col = LAYER_COL[k];
			int sideN = k < 2 ? 18000 : 13500;
			int topN = k < 2 ? 5200 : 3900;
			int inN = k < 2 ? 6000 : 4500;
			for (int i = 0; i < sideN; i++) {
				double th = rnd.nextDouble() * Math.PI * 2;
				cake.add(new P(r * Math.cos(th), r * Math.sin(th), z0 + rnd.nextDouble() * h, col, 3));
			}
			for (int i = 0; i < topN; i++) {
				double th = rnd.nextDouble() * Math.PI * 2;
				double rr = r * Math.sqrt(rnd.nextDouble());
				cake.add(new P(rr * Math.cos(th), rr * Math.sin(th), z0 + h, col, 3));
			}
			int inner = darken(col, 0.10);
			for (int i = 0; i < inN; i++) {
				double th = rnd.nextDouble() * Math.PI * 2;
				double rr = r * (0.93 + 0.05 * rnd.nextDouble());
				cake.add(new P(rr * Math.cos(th), rr * Math.sin(th), z0 + rnd.nextDouble() * h, inner, 3));
			}
			int cream = lighten(col, 0.55);
			for (int b = 0; b < 26; b++) {
				double ang = 2 * Math.PI * (b + 0.5) / 26;
				double bx = r * 1.02 * Math.cos(ang);
				double by = r * 1.02 * Math.sin(ang);
				double bz = z0 + h - 0.05;
				for (int i = 0; i < 210; i++) {
					double u = rnd.nextDouble() * Math.PI * 2;
					double v = Math.acos(rnd.nextDouble() * 2 - 1);
					double rr2 = 0.078 * Math.cbrt(rnd.nextDouble());
					cake.add(new P(bx + Math.sin(v) * Math.cos(u) * rr2, by + Math.sin(v) * Math.sin(u) * rr2,
							bz + Math.cos(v) * rr2, cream, 3));
				}
			}
			z0 += h;
		}
		cakeTop = z0;

		buildDigits();
		buildFlame();
		buildSprinkles();
		buildConfetti();
		buildStars();
	}

	private void buildStars() {
		starN = 0;
		starX = new double[starN];
		starY = new double[starN];
		starR = new double[starN];
		starP = new double[starN];
		starC = new int[starN];
		for (int i = 0; i < starN; i++) {
			starX[i] = rnd.nextDouble() * W;
			starY[i] = rnd.nextDouble() * H;
			starR[i] = 0.9 + rnd.nextDouble() * 2.1;
			starP[i] = rnd.nextDouble() * Math.PI * 2;
			starC[i] = CONFETTI_COL[rnd.nextInt(CONFETTI_COL.length)];
		}
		bokN = 0;
		bokX = new double[bokN];
		bokY = new double[bokN];
		bokR = new double[bokN];
		bokC = new int[bokN];
		for (int i = 0; i < bokN; i++) {
			bokX[i] = rnd.nextDouble() * W;
			bokY[i] = rnd.nextDouble() * H;
			bokR[i] = 7 + rnd.nextDouble() * 12;
			bokC[i] = CONFETTI_COL[rnd.nextInt(CONFETTI_COL.length)];
		}
	}

	private void drawStars(Graphics2D g2) {
		if (starX == null) {
			return;
		}
		for (int i = 0; i < starN; i++) {
			double a = 0.30 + 0.55 * (0.5 + 0.5 * Math.sin(frameCount * 0.05 + starP[i]));
			int col = starC[i];
			g2.setColor(new Color((col >> 16) & 255, (col >> 8) & 255, col & 255, (int) (a * 200)));
			double r = starR[i];
			g2.fillOval((int) (starX[i] - r), (int) (starY[i] - r), (int) (r * 2), (int) (r * 2));
		}
		for (int i = 0; i < bokN; i++) {
			int col = bokC[i];
			g2.setColor(new Color((col >> 16) & 255, (col >> 8) & 255, col & 255, 26));
			double r = bokR[i];
			g2.fillOval((int) (bokX[i] - r), (int) (bokY[i] - r), (int) (r * 2), (int) (r * 2));
		}
	}

	private void buildDigits() {
		int imgW = 560;
		int imgH = 340;
		BufferedImage text = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_ARGB);
		Graphics2D tg = text.createGraphics();
		tg.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		tg.setFont(new Font("Microsoft YaHei", Font.BOLD, 240));
		tg.setColor(Color.WHITE);
		FontMetrics fm = tg.getFontMetrics();
		int tw = fm.stringWidth(ageStr);
		tg.drawString(ageStr, (imgW - tw) / 2, imgH / 2 + fm.getAscent() / 2 - 18);
		tg.dispose();

		int minX = imgW;
		int maxX = 0;
		int minY = imgH;
		int maxY = 0;
		for (int y = 0; y < imgH; y++) {
			for (int x = 0; x < imgW; x++) {
				if ((text.getRGB(x, y) >>> 24) > 120) {
					if (x < minX) {
						minX = x;
					}
					if (x > maxX) {
						maxX = x;
					}
					if (y < minY) {
						minY = y;
					}
					if (y > maxY) {
						maxY = y;
					}
				}
			}
		}
		double bh = maxY - minY + 1;
		double bw = maxX - minX + 1;
		double height = 0.85;
		double scale = height / bh;
		double maxW = LAYERS[4][0] * 1.03 * 2 * 0.70;
		if (bw * scale > maxW) {
			scale = maxW / bw;
		}
		double yaw = Math.toRadians(YAW);
		double cy = Math.cos(yaw);
		double sy = Math.sin(yaw);
		double mx = (minX + maxX) / 2.0;
		double my = (minY + maxY) / 2.0;
		double thick = 0.14;
		int slices = 5;
		for (int y = minY; y <= maxY; y += 3) {
			for (int x = minX; x <= maxX; x += 3) {
				if ((text.getRGB(x, y) >>> 24) <= 120) {
					continue;
				}
				double nx = (x - mx) * scale + (rnd.nextDouble() - 0.5) * 0.006;
				double nz = (my - y) * scale + height / 2.0 + (rnd.nextDouble() - 0.5) * 0.006;
				for (int L = 0; L < slices; L++) {
					double t = thick * L / (slices - 1);
					double shade = 0.74 + 0.26 * L / (slices - 1);
					int col = shadeOf(0xFF8FA3, shade);
					digits.add(new P(nx * cy + t * sy, nx * sy - t * cy, cakeTop + nz, col, 2));
				}
			}
		}
		flameBase = cakeTop + height + 0.03;
	}

	private void buildFlame() {
		for (int i = 0; i < 3200; i++) {
			flameOuter.add(flamePoint(0.30, 0.075, 0xFF4A0F));
		}
		for (int i = 0; i < 2200; i++) {
			flameInner.add(flamePoint(0.22, 0.042, 0xFFE9A8));
		}
	}

	private P flamePoint(double h, double mr, int col) {
		double t = rnd.nextDouble();
		double r = mr * Math.sin(Math.PI * Math.pow(t, 0.45)) * (1 - 0.75 * t) * Math.sqrt(rnd.nextDouble());
		double th = rnd.nextDouble() * Math.PI * 2;
		return new P(r * Math.cos(th), r * Math.sin(th), flameBase + t * h, col, 1);
	}

	private void buildSprinkles() {
		int[] pal = { 0xFF8FA3, 0x7EC8E3, 0xFFC857, 0x9AE6B4, 0xC79BFF, 0xFFFFFF };
		double tr = LAYERS[4][0] * 1.03;
		for (int k = 0; k < 30; k++) {
			double rad = tr * Math.sqrt(rnd.nextDouble()) * 0.92;
			double ang = rnd.nextDouble() * Math.PI * 2;
			double bx = rad * Math.cos(ang);
			double by = rad * Math.sin(ang);
			int col = pal[rnd.nextInt(pal.length)];
			for (int i = 0; i < 50; i++) {
				double u = rnd.nextDouble() * Math.PI * 2;
				double v = Math.acos(rnd.nextDouble() * 2 - 1);
				cake.add(new P(bx + Math.sin(v) * Math.cos(u) * 0.04, by + Math.sin(v) * Math.sin(u) * 0.04,
						cakeTop + 0.02 + Math.cos(v) * 0.03, col, 1));
			}
		}
	}

	private void buildConfetti() {
		for (int i = 0; i < 4200; i++) {
			confetti.add(new P(rnd.nextDouble() * 27 - 13.5, rnd.nextDouble() * 19 - 9.5, -2 + rnd.nextDouble() * 13,
					CONFETTI_COL[rnd.nextInt(CONFETTI_COL.length)], 2 + rnd.nextInt(3)));
		}
	}

	private static int lighten(int rgb, double a) {
		int r = (int) (((rgb >> 16) & 255) + (255 - ((rgb >> 16) & 255)) * a);
		int g = (int) (((rgb >> 8) & 255) + (255 - ((rgb >> 8) & 255)) * a);
		int b = (int) ((rgb & 255) + (255 - (rgb & 255)) * a);
		return (r << 16) | (g << 8) | b;
	}

	private double camX, camY, camZ, fwdX, fwdY, fwdZ, rgtX, rgtY, rgtZ, upX, upY, upZ;
	private double depth;

	private void setupCamera() {
		double dx = CPOS[0] - CTGT[0];
		double dy = CPOS[1] - CTGT[1];
		double dz = CPOS[2] - CTGT[2];
		double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
		dx /= len;
		dy /= len;
		dz /= len;
		double dist = len / CZOOM;
		camX = CTGT[0] + dx * dist;
		camY = CTGT[1] + dy * dist;
		camZ = CTGT[2] + dz * dist;
		fwdX = -dx;
		fwdY = -dy;
		fwdZ = -dz;
		rgtX = fwdY * CUP[2] - fwdZ * CUP[1];
		rgtY = fwdZ * CUP[0] - fwdX * CUP[2];
		rgtZ = fwdX * CUP[1] - fwdY * CUP[0];
		double rl = Math.sqrt(rgtX * rgtX + rgtY * rgtY + rgtZ * rgtZ);
		rgtX /= rl;
		rgtY /= rl;
		rgtZ /= rl;
		upX = rgtY * fwdZ - rgtZ * fwdY;
		upY = rgtZ * fwdX - rgtX * fwdZ;
		upZ = rgtX * fwdY - rgtY * fwdX;
	}

	private boolean project(double x, double y, double z, int[] out) {
		double vx = x - camX;
		double vy = y - camY;
		double vz = z - camZ;
		double zc = vx * fwdX + vy * fwdY + vz * fwdZ;
		if (zc < 0.5) {
			return false;
		}
		double xc = vx * rgtX + vy * rgtY + vz * rgtZ;
		double yc = vx * upX + vy * upY + vz * upZ;
		out[0] = (int) Math.round(W / 2.0 + (xc / zc) * FOCAL);
		out[1] = (int) Math.round(H / 2.0 - (yc / zc) * FOCAL);
		depth = zc;
		return true;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		step();
		repaint();
	}

	private void step() {
		frameCount++;
		int fr = frameCount;
		if (fr >= HBFADE && !bstart) {
			bstart = true;
			for (Ribbon r : ribbons) {
				r.x = 0;
				r.y = 0;
				r.z = 2.4;
				r.alive = true;
				r.birth = fr;
				double th = rnd.nextDouble() * Math.PI * 2;
				double ph = Math.acos(rnd.nextDouble() * 2 - 1);
				double sp = 0.22 + rnd.nextDouble() * 0.18;
				r.vx = sp * Math.sin(ph) * Math.cos(th);
				r.vy = sp * Math.sin(ph) * Math.sin(th);
				r.vz = sp * Math.cos(ph) * 0.9 + 0.20;
			}
		}
		if (bstart) {
			for (Ribbon r : ribbons) {
				if (!r.alive) {
					continue;
				}
				r.vz -= 0.012;
				r.x += r.vx;
				r.y += r.vy;
				r.z += r.vz;
				r.phase += 0.10;
				if (fr - r.birth > 300) {
					r.alive = false;
				}
			}
			for (P c : confetti) {
				c.z -= 0.12;
				if (c.z < -2) {
					c.x = rnd.nextDouble() * 27 - 13.5;
					c.y = rnd.nextDouble() * 19 - 9.5;
					c.z = 11;
				}
			}
		}
		if (fr >= CAKEF) {
			rot += RSPD;
		}
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (off == null) {
			off = new java.awt.image.BufferedImage(W, H, java.awt.image.BufferedImage.TYPE_INT_ARGB);
		}
		Graphics2D g2 = off.createGraphics();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setComposite(java.awt.AlphaComposite.Clear);
		g2.fillRect(0, 0, W, H);
		g2.setComposite(java.awt.AlphaComposite.SrcOver);

		setupCamera();
		int fr = frameCount;
		if (bstart) {
			if (fr >= CAKEF) {
			}
		}
		if (fr >= CAKEF) {
			drawScene(g2);
		}
		if (fr < CDEND) {
			int cur = CD0 - (fr / SPN);
			int[] ccols = { 0, 0x00E676, 0xFF9100, 0xF50057 };
			double ts = (fr % SPN) / (double) SPN;
			double s = ts < 0.35 ? Math.max(1.0, 1.5 - 1.5 * (ts / 0.35)) : 1.0;
			drawSmoothText(g2, String.valueOf(cur), W / 2.0, H / 2.0, 170 * s, ccols[cur]);
		} else if (fr < HBFADE) {
			double t = fr - CDEND;
			double sc;
			double op;
			if (t < HBPOP - CDEND) {
				double p = t / (double) (HBPOP - CDEND);
				sc = 0.4 + 0.6 * (1 - Math.pow(1 - p, 3));
				op = 1.0;
			} else if (t < HBHOLD - CDEND) {
				sc = 1.0 + 0.03 * Math.sin((t - 10) * 0.3);
				op = 1.0;
			} else {
				double p = Math.min(1.0, (t - (HBHOLD - CDEND)) / (double) (HBFADE - HBHOLD));
				sc = 1.0 + 0.04 * Math.pow(p, 1.6);
				op = Math.max(0.0, 1 - p);
			}
			g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) op));
			double hbH = 100 * sc;
			drawSmoothText(g2, "Happy Birthday", W / 2.0, 450, hbH, 0xFF69B4);
			g2.setComposite(AlphaComposite.SrcOver);
		}
		g2.dispose();
		Graphics2D gs = (Graphics2D) g.create();
		gs.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		gs.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		gs.setColor(new Color(0x0B1026));
		gs.fillRect(0, 0, getWidth(), getHeight());
		double fit = Math.min(getWidth() / (double) W, getHeight() / (double) H) * 0.94;
		int dw = (int) Math.round(W * fit);
		int dh = (int) Math.round(H * fit);
		double ox = (getWidth() - dw) / 2.0;
		double oy = (getHeight() - dh) / 2.0;
		drawWindowConfetti(gs, fit, ox, oy);
		if (bstart) {
			drawRibbons(gs, fit, ox, oy);
		}
		gs.drawImage(off, (getWidth() - dw) / 2, (getHeight() - dh) / 2, dw, dh, null);
		float rad = (float) (Math.max(getWidth(), getHeight()) * 0.72);
		gs.setPaint(
				new java.awt.RadialGradientPaint(new java.awt.geom.Point2D.Double(getWidth() / 2.0, getHeight() * 0.52),
						rad, new float[] { 0f, 1f }, new Color[] { new Color(30, 40, 90, 34), new Color(0, 0, 0, 0) }));
		gs.fillRect(0, 0, getWidth(), getHeight());
		gs.dispose();
	}

	private void drawBackgroundRadial(Graphics2D g2) {
		java.awt.RadialGradientPaint rgp = new java.awt.RadialGradientPaint(
				new java.awt.geom.Point2D.Double(W / 2.0, 470), 620f, new float[] { 0f, 1f },
				new Color[] { new Color(30, 40, 90, 40), new Color(0, 0, 0, 0) });
		g2.setPaint(rgp);
		g2.fillRect(0, 0, W, H);
	}

	private void drawWindowConfetti(Graphics2D gs, double fit, double cx, double cy) {
		if (!bstart) {
			return;
		}
		for (P c : confetti) {
			if (!project(c.x, c.y, c.z, tmp)) {
				continue;
			}
			int sx = (int) Math.round(cx + tmp[0] * fit);
			int sy = (int) Math.round(cy + tmp[1] * fit);
			if (sx < -6 || sx > getWidth() + 6 || sy < -6 || sy > getHeight() + 6) {
				continue;
			}
			gs.setColor(new Color(c.col));
			int sz = Math.max(2, (int) Math.round(c.size * fit));
			gs.fillOval(sx - sz / 2, sy - sz / 2, sz, sz);
		}
	}

	private void drawConfetti(Graphics2D g2) {
		for (P c : confetti) {
			if (!project(c.x, c.y, c.z, tmp)) {
				continue;
			}
			g2.setColor(new Color(c.col));
			int sz = Math.max(2, c.size);
			g2.fillOval(tmp[0] - sz / 2, tmp[1] - sz / 2, sz, sz);
		}
	}

	private void drawRibbons(Graphics2D g2, double fit, double cx, double cy) {
		g2.setStroke(new BasicStroke((float) (2.6 * fit), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		for (Ribbon r : ribbons) {
			if (!r.alive) {
				continue;
			}
			if (!project(r.x, r.y, r.z, tmp)) {
				continue;
			}
			double sp = Math.hypot(r.vx, r.vy);
			if (sp < 1e-6) {
				continue;
			}
			double ux = r.vx / sp;
			double uy = r.vy / sp;
			double nx = -uy;
			double ny = ux;
			double len = 0.55;
			int seg = 8;
			Path2D.Double path = new Path2D.Double();
			boolean begun = false;
			int lx = 0;
			int ly = 0;
			double acc = 0.0;
			for (int i = 0; i <= seg; i++) {
				double t = i / (double) seg;
				double wob = Math.sin(r.phase + t * 3.0) * 0.10;
				double px = r.x + ux * (t * len) + nx * wob;
				double py = r.y + uy * (t * len) + ny * wob;
				double pz = r.z + r.vz / sp * (t * len);
				if (!project(px, py, pz, tmp)) {
					begun = false;
					continue;
				}
				int sx = (int) Math.round(cx + tmp[0] * fit);
				int sy = (int) Math.round(cy + tmp[1] * fit);
				if (!begun) {
					path.moveTo(sx, sy);
					lx = sx;
					ly = sy;
					begun = true;
				} else {
					double d = Math.hypot(sx - lx, sy - ly);
					if (acc + d > 108.0 * fit) {
						break;
					}
					acc += d;
					path.lineTo(sx, sy);
					lx = sx;
					ly = sy;
				}
			}
			g2.setColor(new Color(r.col));
			g2.draw(path);
		}
	}

	private void drawScene(Graphics2D g2) {
		int total = cake.size() + digits.size() + flameOuter.size() + flameInner.size();
		if (buf == null) {
			buf = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
			pixels = ((DataBufferInt) buf.getRaster().getDataBuffer()).getData();
			px = new int[total];
			py = new int[total];
			pc = new int[total];
			ps = new int[total];
			nxt = new int[total];
		}
		java.util.Arrays.fill(pixels, 0);
		java.util.Arrays.fill(binHead, -1);

		double c = Math.cos(Math.toRadians(rot));
		double s = Math.sin(Math.toRadians(rot));
		int idx = 0;
		idx = addAll(cake, c, s, idx);
		idx = addAll(digits, c, s, idx);
		idx = addAll(flameOuter, c, s, idx);
		idx = addAll(flameInner, c, s, idx);

		for (int b = 0; b < 256; b++) {
			for (int i = binHead[b]; i >= 0; i = nxt[i]) {
				int sx = px[i];
				int sy = py[i];
				int col = pc[i];
				int sz = ps[i];
				int r = (col >> 16) & 255;
				int gg = (col >> 8) & 255;
				int bb = col & 255;
				if (sz == 1) {
					if (sx >= 0 && sx < W && sy >= 0 && sy < H) {
						pixels[sy * W + sx] = 0xFF000000 | (r << 16) | (gg << 8) | bb;
					}
					continue;
				}
				for (int yy = sy; yy < sy + sz; yy++) {
					if (yy < 0 || yy >= H) {
						continue;
					}
					int row = yy * W;
					for (int xx = sx; xx < sx + sz; xx++) {
						if (xx < 0 || xx >= W) {
							continue;
						}
						pixels[row + xx] = 0xFF000000 | (r << 16) | (gg << 8) | bb;
					}
				}
			}
		}
		g2.drawImage(buf, 0, 0, null);
	}

	private int addAll(ArrayList<P> list, double c, double s, int idx) {
		for (P p : list) {
			double rx = p.x * c - p.y * s;
			double ry = p.x * s + p.y * c;
			if (!project(rx, ry, p.z, tmp)) {
				continue;
			}
			int sx = tmp[0];
			int sy = tmp[1];
			if (sx < -40 || sx > W + 40 || sy < -40 || sy > H + 40) {
				continue;
			}
			px[idx] = sx;
			py[idx] = sy;
			pc[idx] = p.col;
			ps[idx] = p.size;
			int b = (int) ((20.0 - depth) / 14.0 * 256);
			if (b < 0) {
				b = 0;
			}
			if (b > 255) {
				b = 255;
			}
			nxt[idx] = binHead[b];
			binHead[b] = idx;
			idx++;
			if (idx >= px.length) {
				break;
			}
		}
		return idx;
	}

	private double drawSmoothText(Graphics2D g2, String text, double cx, double cy, double height, int col) {
		return drawSmoothText(g2, text, cx, cy, height, col, true);
	}

	private double drawSmoothText(Graphics2D g2, String text, double cx, double cy, double height, int col,
			boolean bold) {
		int style = bold ? Font.BOLD : Font.PLAIN;
		int pad = 24;
		int fs = 200;
		BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		Graphics2D pg = probe.createGraphics();
		pg.setFont(new Font("Microsoft YaHei", style, fs));
		java.awt.FontMetrics fm = pg.getFontMetrics();
		int tw = fm.stringWidth(text);
		int asc = fm.getAscent();
		int dsc = fm.getDescent();
		pg.dispose();
		int imgW = tw + pad * 2;
		int imgH = asc + dsc + pad * 2;
		BufferedImage img = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_ARGB);
		Graphics2D tg = img.createGraphics();
		tg.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		tg.setFont(new Font("Microsoft YaHei", style, fs));
		tg.setColor(new Color(col));
		tg.drawString(text, pad, pad + asc);
		tg.dispose();

		int inkMinX = imgW;
		int inkMaxX = -1;
		int inkMinY = imgH;
		int inkMaxY = -1;
		for (int y = 0; y < imgH; y++) {
			for (int x = 0; x < imgW; x++) {
				if ((img.getRGB(x, y) >>> 24) > 24) {
					if (x < inkMinX) {
						inkMinX = x;
					}
					if (x > inkMaxX) {
						inkMaxX = x;
					}
					if (y < inkMinY) {
						inkMinY = y;
					}
					if (y > inkMaxY) {
						inkMaxY = y;
					}
				}
			}
		}
		if (inkMaxX < 0) {
			return 0;
		}
		double sc = height / (inkMaxY - inkMinY + 1);
		int dw = (int) Math.round(imgW * sc);
		int dh = (int) Math.round(imgH * sc);
		int dx = (int) Math.round(cx - (inkMinX + inkMaxX + 1) / 2.0 * sc);
		int dy = (int) Math.round(cy - (inkMinY + inkMaxY + 1) / 2.0 * sc);
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2.drawImage(img, dx, dy, dw, dh, null);
		return height;
	}

	private static int darken(int rgb, double f) {
		int r = (int) (((rgb >> 16) & 255) * (1 - f));
		int g = (int) (((rgb >> 8) & 255) * (1 - f));
		int b = (int) ((rgb & 255) * (1 - f));
		return (r << 16) | (g << 8) | b;
	}

	private static int shadeOf(int rgb, double f) {
		int r = Math.min(255, (int) (((rgb >> 16) & 255) * f));
		int g = Math.min(255, (int) (((rgb >> 8) & 255) * f));
		int b = Math.min(255, (int) ((rgb & 255) * f));
		return (r << 16) | (g << 8) | b;
	}

	private static java.awt.image.BufferedImage off = null;

	public java.awt.Dimension getPreferredSize() {
		return new java.awt.Dimension(W, H);
	}

	public static void main(String[] args) {
		String age = args.length > 0 ? args[0] : null;
		if (age == null) {
			age = JOptionPane.showInputDialog(null, "宝宝多少岁生日？", "");
		}
		age = age == null ? "" : age.replaceAll("[^0-9]", "");
		if (age.isEmpty()) {
			age = "";
		}
		if (age.length() > 2) {
			age = age.substring(0, 2);
		}
		final String a = age;
		final Heart3[] holder = new Heart3[1];
		javax.swing.SwingUtilities.invokeLater(() -> {
			JFrame f = new JFrame("生日蛋糕");
			Heart3 p = new Heart3(a);
			holder[0] = p;
			f.setContentPane(p);
			java.awt.Dimension scr = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
			double ds = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
					.getDefaultConfiguration().getDefaultTransform().getScaleX();
			if (ds <= 0) {
				ds = 1.0;
			}
			java.awt.DisplayMode dm = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
					.getDefaultScreenDevice().getDisplayMode();
			int fw = (int) Math.round(dm.getWidth() / ds * 0.69);
			int fh = (int) Math.round(dm.getHeight() / ds * 0.69);
			f.setSize(fw, fh);
			f.setLocationRelativeTo(null);
			f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			f.setVisible(true);
			p.requestFocusInWindow();
			p.addKeyListener(new KeyAdapter() {
				@Override
				public void keyPressed(KeyEvent e) {
					if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
						System.exit(0);
					}
					if (e.getKeyCode() == KeyEvent.VK_SPACE) {
						Heart3 q = holder[0];
						q.frameCount = 0;
						q.rot = 0;
						q.bstart = false;
						for (Ribbon r : q.ribbons) {
							r.alive = false;
						}
					}
				}
			});
		});
	}
}
