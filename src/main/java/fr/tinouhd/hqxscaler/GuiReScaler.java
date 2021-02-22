package fr.tinouhd.hqxscaler;

import com.zakgof.velvetvideo.*;
import com.zakgof.velvetvideo.impl.VelvetVideoLib;
import javafx.util.Pair;

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
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.*;

final class GuiReScaler extends ReScaler
{
	private JProgressBar progressBarFiles;
	private JProgressBar progressBarAnimation;
	public GuiReScaler(int scale, JProgressBar progress, JProgressBar animation)
	{
		super(scale);
		this.progressBarFiles = progress;
		this.progressBarAnimation = animation;
	}

	@Override
	public void processVideo(File video, File out) throws InterruptedException, ExecutionException
	{
		IVelvetVideoLib lib = VelvetVideoLib.getInstance();

		IDemuxer demuxer = lib.demuxer(video);

		IMuxerBuilder builder = lib.muxer("mp4").videoEncoder(lib.videoEncoder("libx264").bitrate(800000));
		/*WIP: demuxer.audioStreams().forEach(ad -> {
			builder.audioEncoder(lib.audioEncoder("aac", ad.properties().format()));
		});*/
		IMuxer muxer = builder.build(out);


		IVideoEncoderStream encoder = muxer.videoEncoder(0);
		IVideoDecoderStream videoStream = demuxer.videoStream(0);

		/*WIP: List<IAudioDecoderStream> audioDecoders = (List<IAudioDecoderStream>) demuxer.audioStreams();
		List<IAudioEncoderStream> audioEncoders = new ArrayList<>();
		audioDecoders.forEach(ad -> audioEncoders.add(muxer.audioEncoder(ad.index())));*/

		ThreadPoolExecutor collectorExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
		Queue<Future<Pair<String, BufferedImage>>> futures = new ArrayDeque<>();
		progressBarAnimation.setMaximum((int) videoStream.properties().frames());
		progressBarAnimation.setValue(0);
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
					//WIP: audioEncoders.forEach(ae -> ae.encode(audioDecoders.get(audioEncoders.indexOf(ae)).nextFrame().samples()));
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
		muxer.close();
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

	@Override protected void process(File f) throws UnsupportedOperationException
	{
		try
		{
			super.process(f);
		}catch (UnsupportedOperationException e)
		{
			System.err.println("Error file \"" + f.getName() + "\" not supported !");
		}finally
		{
			if(!f.isDirectory())
			{
				progressBarFiles.setValue(progressBarFiles.getValue() + 1);
			}
		}
	}

	@Override public void processFileAndSave(File f)
	{
		if (f.isDirectory())
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
