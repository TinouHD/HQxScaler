package fr.tinouhd.hqxscaler.hqx;

abstract class Hqx
{
	private static final int Ymask = 0x00FF0000;
	private static final int Umask = 0x0000FF00;
	private static final int Vmask = 0x000000FF;

	/**
	 * Compares two ARGB colors according to the provided Y, U, V and A thresholds.
	 *
	 * @param c1  an ARGB color
	 * @param c2  a second ARGB color
	 * @param trY the Y (luminance) threshold
	 * @param trU the U (chrominance) threshold
	 * @param trV the V (chrominance) threshold
	 * @param trA the A (transparency) threshold
	 * @return true if colors differ more than the thresholds permit, false otherwise
	 */
	protected static boolean diff(final int c1, final int c2, final int trY, final int trU, final int trV, final int trA)
	{
		final int YUV1 = RgbYuv.getYuv(c1);
		final int YUV2 = RgbYuv.getYuv(c2);

		return ((Math.abs((YUV1 & Ymask) - (YUV2 & Ymask)) > trY) || (Math.abs((YUV1 & Umask) - (YUV2 & Umask)) > trU) || (Math.abs((YUV1 & Vmask) - (YUV2 & Vmask)) > trV) || (Math.abs(((c1 >> 24) - (c2 >> 24))) > trA));
	}

}
