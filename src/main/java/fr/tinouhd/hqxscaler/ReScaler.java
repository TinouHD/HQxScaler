package fr.tinouhd.hqxscaler;

import fr.tinouhd.hqxscaler.hqx.Hqx_2x;
import fr.tinouhd.hqxscaler.hqx.Hqx_3x;
import fr.tinouhd.hqxscaler.hqx.Hqx_4x;
import fr.tinouhd.hqxscaler.hqx.RgbYuv;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;

public class ReScaler
{
	private BufferedImage bi;
	private int scale;

	public ReScaler(BufferedImage bi, int scale)
	{
		this.bi = bi;
		this.scale = scale;
	}

	public BufferedImage getReScaledImage()
	{
		return reScaler();
	}

	public void saveToFile()
	{
		File out = new File("out");
		if (!out.exists())
			out.mkdirs();
		BufferedImage dest = reScaler();
		if (dest == null)
			return;
		try
		{
			ImageIO.write(dest, "PNG", new File(out, System.currentTimeMillis() + "_x" + scale + ".png"));
		} catch (java.io.IOException ignored)
		{
		}
	}

	private BufferedImage reScaler()
	{
		if (scale == 1)
			return bi;
		else if (scale < 2)
			throw new IllegalArgumentException();
		else if (scale > 4)
			throw new IllegalArgumentException();
		else
		{
			if (bi != null)
			{
				// Convert image to ARGB if on another format
				if (bi.getType() != BufferedImage.TYPE_INT_ARGB && bi.getType() != BufferedImage.TYPE_INT_ARGB_PRE)
				{
					final BufferedImage temp = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_ARGB);
					temp.getGraphics().drawImage(bi, 0, 0, null);
					bi = temp;
				}
				// Obtain pixel data for source image
				final int[] data = ((DataBufferInt) bi.getRaster().getDataBuffer()).getData();

				// Initialize lookup tables
				RgbYuv.hqxInit();

				// Create the destination image, twice as large for 2x algorithm
				final BufferedImage biDest = new BufferedImage(bi.getWidth() * scale, bi.getHeight() * scale, BufferedImage.TYPE_INT_ARGB);
				// Obtain pixel data for destination image
				final int[] dataDest2 = ((DataBufferInt) biDest.getRaster().getDataBuffer()).getData();
				// Resize it
				switch (scale)
				{
					default:
					case 2:
						Hqx_2x.hq2x_32_rb(data, dataDest2, bi.getWidth(), bi.getHeight());
						break;
					case 3:
						Hqx_3x.hq3x_32_rb(data, dataDest2, bi.getWidth(), bi.getHeight());
						break;
					case 4:
						Hqx_4x.hq4x_32_rb(data, dataDest2, bi.getWidth(), bi.getHeight());
						break;
				}
				// Save our result
				return biDest;
			}
		}
		return null;
	}
}
