package fr.tinouhd.hqxscaler.hqx;

/**
 * Helper class to interpolate colors. Nothing to see here, move along...
 */
final class Interpolation
{
	private static final int Mask4 = 0xFF000000;
	private static final int Mask2 = 0x0000FF00;
	private static final int Mask13 = 0x00FF00FF;

	private Interpolation()
	{
		throw new UnsupportedOperationException();
	}

	// return statements:
	//	 1. line: green
	//	 2. line: red and blue
	//	 3. line: alpha

	static int Mix3To1(final int c1, final int c2)
	{
		//return (c1*3+c2) >> 2;
		if (c1 == c2)
		{
			return c1;
		}
		return ((((c1 & Mask2) * 3 + (c2 & Mask2)) >> 2) & Mask2) | ((((c1 & Mask13) * 3 + (c2 & Mask13)) >> 2) & Mask13) | ((((c1 & Mask4) >> 2) * 3 + ((c2 & Mask4) >> 2)) & Mask4);
	}

	static int Mix2To1To1(final int c1, final int c2, final int c3)
	{
		//return (c1*2+c2+c3) >> 2;
		return ((((c1 & Mask2) * 2 + (c2 & Mask2) + (c3 & Mask2)) >> 2) & Mask2) | ((((c1 & Mask13) * 2 + (c2 & Mask13) + (c3 & Mask13)) >> 2) & Mask13) | ((((c1 & Mask4) >> 2) * 2 + ((c2 & Mask4) >> 2) + ((c3 & Mask4) >> 2)) & Mask4);
	}

	static int Mix7To1(final int c1, final int c2)
	{
		//return (c1*7+c2)/8;
		if (c1 == c2)
		{
			return c1;
		}
		return ((((c1 & Mask2) * 7 + (c2 & Mask2)) >> 3) & Mask2) | ((((c1 & Mask13) * 7 + (c2 & Mask13)) >> 3) & Mask13) | ((((c1 & Mask4) >> 3) * 7 + ((c2 & Mask4) >> 3)) & Mask4);
	}

	static int Mix2To7To7(final int c1, final int c2, final int c3)
	{
		//return (c1*2+(c2+c3)*7)/16;
		return ((((c1 & Mask2) * 2 + (c2 & Mask2) * 7 + (c3 & Mask2) * 7) >> 4) & Mask2) | ((((c1 & Mask13) * 2 + (c2 & Mask13) * 7 + (c3 & Mask13) * 7) >> 4) & Mask13) | ((((c1 & Mask4) >> 4) * 2 + ((c2 & Mask4) >> 4) * 7 + ((c3 & Mask4) >> 4) * 7) & Mask4);
	}

	static int MixEven(final int c1, final int c2)
	{
		//return (c1+c2) >> 1;
		if (c1 == c2)
		{
			return c1;
		}
		return ((((c1 & Mask2) + (c2 & Mask2)) >> 1) & Mask2) | ((((c1 & Mask13) + (c2 & Mask13)) >> 1) & Mask13) | ((((c1 & Mask4) >> 1) + ((c2 & Mask4) >> 1)) & Mask4);
	}

	static int Mix4To2To1(final int c1, final int c2, final int c3)
	{
		//return (c1*5+c2*2+c3)/8;
		return ((((c1 & Mask2) * 5 + (c2 & Mask2) * 2 + (c3 & Mask2)) >> 3) & Mask2) | ((((c1 & Mask13) * 5 + (c2 & Mask13) * 2 + (c3 & Mask13)) >> 3) & Mask13) | ((((c1 & Mask4) >> 3) * 5 + ((c2 & Mask4) >> 3) * 2 + ((c3 & Mask4) >> 3)) & Mask4);
	}

	static int Mix6To1To1(final int c1, final int c2, final int c3)
	{
		//return (c1*6+c2+c3)/8;
		return ((((c1 & Mask2) * 6 + (c2 & Mask2) + (c3 & Mask2)) >> 3) & Mask2) | ((((c1 & Mask13) * 6 + (c2 & Mask13) + (c3 & Mask13)) >> 3) & Mask13) | ((((c1 & Mask4) >> 3) * 6 + ((c2 & Mask4) >> 3) + ((c3 & Mask4) >> 3)) & Mask4);
	}

	static int Mix5To3(final int c1, final int c2)
	{
		//return (c1*5+c2*3)/8;
		if (c1 == c2)
		{
			return c1;
		}
		return ((((c1 & Mask2) * 5 + (c2 & Mask2) * 3) >> 3) & Mask2) | ((((c1 & Mask13) * 5 + (c2 & Mask13) * 3) >> 3) & Mask13) | ((((c1 & Mask4) >> 3) * 5 + ((c2 & Mask4) >> 3) * 3) & Mask4);
	}

	static int Mix2To3To3(final int c1, final int c2, final int c3)
	{
		//return (c1*2+(c2+c3)*3)/8;
		return ((((c1 & Mask2) * 2 + (c2 & Mask2) * 3 + (c3 & Mask2) * 3) >> 3) & Mask2) | ((((c1 & Mask13) * 2 + (c2 & Mask13) * 3 + (c3 & Mask13) * 3) >> 3) & Mask13) | ((((c1 & Mask4) >> 3) * 2 + ((c2 & Mask4) >> 3) * 3 + ((c3 & Mask4) >> 3) * 3) & Mask4);
	}

	static int Mix14To1To1(final int c1, final int c2, final int c3)
	{
		//return (c1*14+c2+c3)/16;
		return ((((c1 & Mask2) * 14 + (c2 & Mask2) + (c3 & Mask2)) >> 4) & Mask2) | ((((c1 & Mask13) * 14 + (c2 & Mask13) + (c3 & Mask13)) >> 4) & Mask13) | ((((c1 & Mask4) >> 4) * 14 + ((c2 & Mask4) >> 4) + ((c3 & Mask4) >> 4)) & Mask4);
	}
}
