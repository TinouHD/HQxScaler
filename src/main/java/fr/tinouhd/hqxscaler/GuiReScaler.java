package fr.tinouhd.hqxscaler;

import com.zakgof.velvetvideo.*;
import com.zakgof.velvetvideo.impl.VelvetVideoLib;
import fr.tinouhd.hqxscaler.utils.Pair;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

final class GuiReScaler extends ReScaler
{
	private JProgressBar progressBarFiles;
	private JProgressBar progressBarAnimation;
	public GuiReScaler(int scale, JProgressBar progress, JProgressBar animation)
	{
		super(scale, true);
		this.progressBarFiles = progress;
		this.progressBarAnimation = animation;
	}

	@Override
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

		Queue<Future<fr.tinouhd.hqxscaler.utils.Pair<String, BufferedImage>>> futures = new ArrayDeque<>();
		progressBarAnimation.setMaximum((int) videoDecoder.properties().frames());
		progressBarAnimation.setValue(0);

		AtomicBoolean isAllTreated = new AtomicBoolean(false);

		processThread = new Thread(() -> {
			IDecodedPacket packet;
			for (int i = 0; (packet = demuxer.nextPacket()) != null; i++)
			{
				//final double freeMem = (((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getCommittedVirtualMemorySize()/1000000.0 - 512.0)/1024.0;
				//System.out.println(Runtime.getRuntime().freeMemory() + " | " + freeMem);
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
					progressBarAnimation.setValue(progressBarAnimation.getValue() + 1);
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
		progressBarAnimation.setValue(progressBarAnimation.getMaximum());
	}

	@Override
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
		progressBarAnimation.setMaximum(reader.getNumImages(true));
		progressBarAnimation.setValue(0);
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
					progressBarAnimation.setValue(progressBarAnimation.getValue() + 1);
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
		progressBarAnimation.setValue(progressBarAnimation.getMaximum());
	}

	@Override public void processFileAndSave(File f)
	{
		if (f.isDirectory() && processRoot == null)
		{
			progressBarFiles.setMaximum(countFilesInDirectory(f));
		}else
		{
			progressBarFiles.setMaximum(1);
		}

		super.processFileAndSave(f);
	}

	private static int countFilesInDirectory(File directory) {
		int count = 0;
		for (File file : directory.listFiles()) {
			if (file.isFile()) {
				count++;
			}
			if (file.isDirectory()) {
				count += countFilesInDirectory(file);
			}
		}
		return count;
	}
}
