package fr.tinouhd.hqxscaler;

import fr.tinouhd.hqxscaler.hqx.Hqx_2x;
import fr.tinouhd.hqxscaler.hqx.Hqx_3x;
import fr.tinouhd.hqxscaler.hqx.Hqx_4x;
import fr.tinouhd.hqxscaler.hqx.RgbYuv;
import javafx.util.Pair;
import org.jcodec.api.JCodecException;
import org.jcodec.api.awt.AWTFrameGrab;
import org.jcodec.api.awt.AWTSequenceEncoder;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;

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
				FileChannelWrapper ch = NIOUtils.readableChannel(f);
				MP4Demuxer demuxer = MP4Demuxer.createMP4Demuxer(ch);
				DemuxerTrack video = demuxer.getVideoTrack();
				System.out.println(video.getMeta().getTotalFrames());
				//Queue<Pair<String, BufferedImage>> biq = new ArrayDeque<>();
				File out = new File("out", f.getName().split("\\.")[0] + ".mp4");
				AWTSequenceEncoder encoder = AWTSequenceEncoder.createSequenceEncoder(out, (int)(video.getMeta().getTotalFrames() / video.getMeta().getTotalDuration()));
				ThreadPoolExecutor collectorExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
				Queue<Future<Pair<String, BufferedImage>>> futures = new ArrayDeque<>();
				for (int i = 0; i < video.getMeta().getTotalFrames(); i++)
				{
					int finalI = i;
					collectorExecutor.submit(() -> futures.add(executor.submit(() -> processImage(f.getName().split("\\.")[0] + "#" + finalI, AWTFrameGrab.getFrame(f, finalI)))));
				}
				while(!futures.isEmpty() || collectorExecutor.getActiveCount() > 0)
				{
					if(futures.peek() != null){
						if(futures.peek().isDone()){
							System.out.println("Encoding " + futures.peek().get().getKey());
							encoder.encodeImage(futures.poll().get().getValue());
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
				encoder.finish();

			} catch (IOException | InterruptedException | ExecutionException e)
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

	/*private Queue<Future<BufferedImage>> processVideo(Queue<Pair<String, BufferedImage>> video, Thread t)
	{
		Queue<Future<BufferedImage>> futures = new ArrayDeque<>();
		Thread mainProcessor = new Thread(() -> {
			while (!video.isEmpty() || !t.isInterrupted())
			{
				if (video.peek() != null)
				{
					Pair<String, BufferedImage> pair = video.poll();
					System.out.println("Processing : " + pair.getKey() + "\t|\t" + video.size());
					futures.add(executor.submit(() -> processImage(pair.getKey(), pair.getValue())));
					if(futures.peek() == null)
					{
						//System.out.println("=====================");
					}
				} else
				{
					try
					{
						wait();
					} catch (InterruptedException e)
					{}
				}
			}
			System.out.println("Finished !");
		}, "mainProcessorThread");
		mainProcessor.start();

		return futures;
	}*/

	@Override public void close()
	{
		executor.shutdown();
	}
}
