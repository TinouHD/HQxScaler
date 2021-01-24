package fr.tinouhd.hqxscaler.hqx;

public final class RgbYuv
{
	private static final int rgbMask = 0x00FFFFFF;
	private static int[] RGBtoYUV = new int[0x1000000];

	private RgbYuv()
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * Returns the 24bit YUV equivalent of the provided 24bit RGB color.<b>Any alpha component is dropped</b>
	 *
	 * @param rgb a 24bit rgb color
	 * @return the corresponding 24bit YUV color
	 */
	static int getYuv(final int rgb)
	{
		return RGBtoYUV[rgb & rgbMask];
	}

	/**
	 * Calculates the lookup table. <b>MUST</b> be called (only once) before doing anything else
	 */
	public static void hqxInit()
	{
		/* Initalize RGB to YUV lookup table */
		int r, g, b, y, u, v;
		for (int c = 0x1000000 - 1; c >= 0; c--)
		{
			r = (c & 0xFF0000) >> 16;
			g = (c & 0x00FF00) >> 8;
			b = c & 0x0000FF;
			y = (int) (+0.299d * r + 0.587d * g + 0.114d * b);
			u = (int) (-0.169d * r - 0.331d * g + 0.500d * b) + 128;
			v = (int) (+0.500d * r - 0.419d * g - 0.081d * b) + 128;
			RGBtoYUV[c] = (y << 16) | (u << 8) | v;
		}
	}

	/// <summary>
	/// Releases the reference to the lookup table.
	/// <para>The table has to be calculated again for the next lookup.</para>
	/// </summary>

	/**
	 * Releases the reference to the lookup table. <b>The table has to be calculated again for the next lookup.</b>
	 */
	public static void hqxDeinit()
	{
		RGBtoYUV = null;
	}
}
