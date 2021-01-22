package fr.tinouhd.hqxscaler;

import com.zakgof.velvetvideo.*;
import com.zakgof.velvetvideo.impl.VelvetVideoLib;
import fr.tinouhd.hqxscaler.hqx.Hqx_2x;
import fr.tinouhd.hqxscaler.hqx.Hqx_3x;
import fr.tinouhd.hqxscaler.hqx.Hqx_4x;
import fr.tinouhd.hqxscaler.hqx.RgbYuv;
import javafx.util.Pair;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.*;

public class ReScaler implements AutoCloseable
{
	private final int scale;
	protected final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(5);

	public ReScaler(int scale)
	{
		if(scale > 4 || scale < 1) throw new IllegalArgumentException();
		this.scale = scale;
	}

	public void processFileAndSave(File f)
	{
		if(f.getName().matches("^.*\\.(bmp|gif|jpg|jpeg|png)$"))
		{
			try
			{
				File out = new File("out", f.getName().split("\\.")[0] + ".png");
				out.mkdirs();
				ImageIO.write(executor.submit(() -> processImage(f.getName().split("\\.")[0], ImageIO.read(f))).get().getValue(), "PNG", out);
			} catch (IOException | InterruptedException | ExecutionException ignored)
			{}
		}else if(f.getName().matches("^.*\\.mp4$"))
		{
			try
			{
				processVideo(f);
			} catch (InterruptedException | ExecutionException e)
			{}
		}else
		{
			throw new UnsupportedOperationException();
		}

	}

	public Pair<String, BufferedImage> processImage(String name, BufferedImage bi)
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

		// Create the destination image, twice as large for scalex algorithm
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
		// Return our result
		return new Pair<>(name, biDest);
	}

	public void processVideo(File video) throws InterruptedException, ExecutionException
	{
		IVelvetVideoLib lib = VelvetVideoLib.getInstance();

		File out = new File("out", video.getName().split("\\.")[0] + ".mp4");
		IMuxer muxer = lib.muxer("mp4").videoEncoder(lib.videoEncoder("libx264").bitrate(800000)).build(out);
		IDemuxer demuxer = lib.demuxer(video);

		IVideoEncoderStream encoder = muxer.videoEncoder(0);
		IVideoDecoderStream videoStream = demuxer.videoStream(0);

		IAudioEncoderStream audioEncoder = muxer.audioEncoder(0);
		IAudioDecoderStream audioDecoder = demuxer.audioStream(0);

		ThreadPoolExecutor collectorExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
		Queue<Future<Pair<String, BufferedImage>>> futures = new ArrayDeque<>();
		IVideoFrame videoFrame;
		for (int i = 0; (videoFrame = videoStream.nextFrame()) != null; i++)
		{
			int finalI = i;
			BufferedImage image = videoFrame.image();
			collectorExecutor.submit(() -> futures.add(executor.submit(() -> processImage(video.getName().split("\\.")[0] + "#" + finalI, image))));
		}
		demuxer.close();
		while(!futures.isEmpty() || collectorExecutor.getActiveCount() > 0)
		{
			if(futures.peek() != null){
				if(futures.peek().isDone()){
					System.out.println("Encoding " + futures.peek().get().getKey());
					encoder.encode(futures.poll().get().getValue());
					audioEncoder.encode(audioDecoder.nextFrame().samples());
				}else
				{
					Thread.sleep(1);
				}
			}else
			{
				Thread.sleep(1);
			}
		}
		collectorExecutor.shutdown();
		collectorExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
		muxer.close();
	}

	@Override public void close()
	{
		executor.shutdown();
	}
}
