package fr.tinouhd.hqxscaler;

import com.sun.org.apache.xerces.internal.impl.xpath.regex.RegularExpression;
import com.zakgof.velvetvideo.*;
import com.zakgof.velvetvideo.impl.VelvetVideoLib;
import fr.tinouhd.hqxscaler.hqx.Hqx_2x;
import fr.tinouhd.hqxscaler.hqx.Hqx_3x;
import fr.tinouhd.hqxscaler.hqx.Hqx_4x;
import fr.tinouhd.hqxscaler.hqx.RgbYuv;
import fr.tinouhd.hqxscaler.utils.Pair;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ReScaler implements AutoCloseable
{
	private final int scale;
	private boolean errorMessage = false;
	protected File processRoot;
	protected final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(5);
	protected Thread processThread;

	/**
	 * @param scale {@code 2-4} the scale factor for this ReScaler.
	 * @throws IllegalArgumentException When {@code scale} isn't between 2 and 4.
	 */
	public ReScaler(int scale)
	{
		if(scale > 4 || scale < 1) throw new IllegalArgumentException();
		this.scale = scale;
	}

	public ReScaler(int scale, boolean errorMessage)
	{
		this(scale);
		this.errorMessage = errorMessage;
	}

	/**
	 * @param f A File, the file to process with this ReScaler.
	 */
	public void processFileAndSave(File f) throws UnsupportedOperationException
	{
		if(f.isDirectory() && processRoot == null)
		{
			processRoot = f;
		}

		if(f.getName().matches("^.*\\.(bmp|jpg|jpeg|png)$"))
		{
			try
			{
				File out;
				if(processRoot != null)
				{
					out = new File("out/" + f.getParentFile().getAbsolutePath().substring(processRoot.getAbsolutePath().length()).replace("\\", "/"), f.getName().split("\\.")[0] + ".png");
				}else
				{
					out = new File("out/", f.getName().split("\\.")[0] + ".png");
				}
				prepareFile(out);
				ImageIO.write(executor.submit(() -> processImage(f.getName().split("\\.")[0], ImageIO.read(f))).get().getValue(), "PNG", out);
				System.gc();
			} catch (IOException | InterruptedException | ExecutionException ignored)
			{}
		}else if(f.getName().matches("^.*\\.mp4$"))
		{
			try
			{
				File out;
				if(processRoot != null)
				{
					out = new File("out/" + f.getParentFile().getAbsolutePath().substring(processRoot.getAbsolutePath().length()).replace("\\", "/"), f.getName().split("\\.")[0] + ".mp4");
				}else
				{
					out = new File("out/", f.getName());
				}
				prepareFile(out);
				processVideo(f, out);
				System.gc();
			} catch (InterruptedException | ExecutionException | IOException e)
			{}
		}else if(f.getName().matches("^.*\\.gif$"))
		{
			try
			{
				File out;
				if(processRoot != null)
				{
					out = new File("out/" + f.getParentFile().getAbsolutePath().substring(processRoot.getAbsolutePath().length()).replace("\\", "/"), f.getName().split("\\.")[0] + ".gif");
				}else
				{
					out = new File("out/", f.getName());
				}
				prepareFile(out);
				processAnimatedGif(f, out);
				System.gc();
			} catch (IOException | InterruptedException | ExecutionException e)
			{}

		}else if(f.isDirectory())
		{
			for(File files : f.listFiles())
			{
				processFileAndSave(files);
			}
		}else
		{
			if(errorMessage)
			{
				System.err.println("Error file \"" + f.getName() + "\" not supported !");
			}else throw new UnsupportedOperationException();
		}
	}

	/**
	 * @param name the name of the image to process (useful for animation processing).
	 * @param bi the image to process.
	 * @return a pair with the name of the image and the processed image.
	 */
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

	/**
	 * @param video the video file to process.
	 * @param out the output file.
	 * @throws InterruptedException if any thread has interrupted the current thread.
	 * @throws ExecutionException when a future computation threw an exception.
	 */
	public void processVideo(File video, File out) throws InterruptedException, ExecutionException
	{
		IVelvetVideoLib lib = VelvetVideoLib.getInstance();

		IDemuxer demuxer = lib.demuxer(video);
		IMuxerBuilder muxerBuilder = lib.muxer("mp4").videoEncoder(lib.videoEncoder("libx264").bitrate(800000));
		if(demuxer.audioStreams().get(0) != null)
		{
			muxerBuilder.audioEncoder(lib.audioEncoder(demuxer.audioStreams().get(0).properties().codec(), demuxer.audioStreams().get(0).properties().format()));
		}
		IMuxer muxer = muxerBuilder.build(out);

		IVideoEncoderStream videoEncoder = muxer.videoEncoder(0);
		IVideoDecoderStream videoDecoder = demuxer.videoStream(0);

		int index = demuxer.audioStreams().stream().findFirst().get().index();

		IAudioEncoderStream audioEncoder = muxer.audioEncoder(index);
		IAudioDecoderStream audioDecoder = demuxer.audioStreams().get(0);

		Queue<Future<Pair<String, BufferedImage>>> futures = new ArrayDeque<>();

		AtomicBoolean isAllTreated = new AtomicBoolean(false);

		processThread = new Thread(() -> {
			IDecodedPacket packet;
			for (int i = 0; (packet = demuxer.nextPacket()) != null; i++)
			{
				while (100.0 * (double) Runtime.getRuntime().freeMemory() / (double) Runtime.getRuntime().totalMemory() <= 10.0)
				{
					try
					{
						Thread.sleep(1000);
					} catch (InterruptedException e)
					{}
					System.out.println(100.0 * (double) Runtime.getRuntime().freeMemory() / (double) Runtime.getRuntime().totalMemory());
					System.gc(); System.gc();
				}
				if(packet.is(MediaType.Audio) && packet.stream() == audioDecoder)
				{
					audioEncoder.encode(packet.asAudio().samples());
				}else if(packet.is(MediaType.Video) && packet.stream() == videoDecoder)
				{
					int finalI = i;
					BufferedImage image = packet.asVideo().image();
					futures.add(executor.submit(() -> processImage(video.getName().split("\\.")[0] + "#" + finalI, image)));
				}
			}
			processThread.interrupt();
			isAllTreated.set(true);
		});
		processThread.start();
		System.gc();

		while(!futures.isEmpty() || !isAllTreated.get())
		{
			if(futures.peek() != null){
				if(futures.peek().isDone()){
					String name = futures.peek().get().getKey();
					System.out.println("Encoding " + name);
					videoEncoder.encode(futures.poll().get().getValue());
					if(Integer.parseInt(name.split("#")[1]) % 10 < 5) {System.gc(); System.gc();}
				}else
				{
					Thread.sleep(1);
				}
			}else
			{
				Thread.sleep(1);
			}
		}
		muxer.close();
		demuxer.close();
	}

	/**
	 * @param gif the gif file to process.
	 * @param out the output file.
	 * @throws IOException if an I/O error occurs.
	 * @throws InterruptedException if any thread has interrupted the current thread.
	 * @throws ExecutionException when a future computation threw an exception.
	 */
	public void processAnimatedGif(File gif, File out) throws IOException, ExecutionException, InterruptedException
	{
		ImageReader reader = ImageIO.getImageReadersByFormatName("gif").next();
		ImageInputStream in = ImageIO.createImageInputStream(gif);
		reader.setInput(in);

		ImageOutputStream outStream = new FileImageOutputStream(out);
		IIOMetadataNode gceNode = GifSequenceWriter.getNode((IIOMetadataNode) reader.getImageMetadata(0).getAsTree(reader.getImageMetadata(0).getNativeMetadataFormatName()), "GraphicControlExtension");
		GifSequenceWriter writer = new GifSequenceWriter(outStream, BufferedImage.TYPE_INT_ARGB, Integer.parseInt(gceNode.getAttribute("delayTime")), true);
		ThreadPoolExecutor collectorExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
		Queue<Future<Pair<String, BufferedImage>>> futures = new ArrayDeque<>();
		for (int i = 0, count = reader.getNumImages(true); i < count; i++)
		{
			BufferedImage image = reader.read(i);
			int finalI = i;
			collectorExecutor.submit(() -> futures.add(executor.submit(() -> processImage(gif.getName().split("\\.")[0] + "#" + finalI, image))));
		}
		System.gc();
		in.close();
		while (!futures.isEmpty() || collectorExecutor.getActiveCount() > 0)
		{
			if(futures.peek() != null)
			{
				if(futures.peek().isDone())
				{
					System.out.println("Encoding " + futures.peek().get().getKey());
					writer.writeToSequence(futures.poll().get().getValue());
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
		writer.close();
		outStream.close();
	}

	@Override public void close()
	{
		executor.shutdown();
	}

	private static void prepareFile(File file) throws IOException
	{
		file.setReadable(true, false);
		file.setWritable(true, false);
		file.setExecutable(true, false);
		file.getParentFile().mkdirs();
		file.createNewFile();
	}
}
