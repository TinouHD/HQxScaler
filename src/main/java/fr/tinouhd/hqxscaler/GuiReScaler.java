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
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;

final class GuiReScaler extends ReScaler
{
	private JProgressBar progressBar;
	public GuiReScaler(int scale, JProgressBar progress)
	{
		super(scale);
		this.progressBar = progress;
	}

	@Override
	public void processVideo(File video) throws InterruptedException, ExecutionException
	{
		IVelvetVideoLib lib = VelvetVideoLib.getInstance();

		File out = new File("out", video.getName().split("\\.")[0] + ".mp4");
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
		progressBar.setMaximum((int) videoStream.properties().frames());
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
					progressBar.setValue(progressBar.getValue() + 1);
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
		progressBar.setValue(progressBar.getMaximum());
	}

	@Override
	public void processAnimatedGif(File gif) throws IOException, ExecutionException, InterruptedException
	{
		ImageReader reader = ImageIO.getImageReadersByFormatName("gif").next();
		ImageInputStream in = ImageIO.createImageInputStream(gif);
		reader.setInput(in);

		ImageOutputStream out = new FileImageOutputStream(new File("out/", gif.getName()));
		IIOMetadataNode gceNode = GifSequenceWriter.getNode((IIOMetadataNode) reader.getImageMetadata(0).getAsTree(reader.getImageMetadata(0).getNativeMetadataFormatName()), "GraphicControlExtension");
		System.out.println(Integer.parseInt(gceNode.getAttribute("delayTime")));
		GifSequenceWriter writer = new GifSequenceWriter(out, BufferedImage.TYPE_INT_ARGB, Integer.parseInt(gceNode.getAttribute("delayTime")), true);
		ThreadPoolExecutor collectorExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
		Queue<Future<Pair<String, BufferedImage>>> futures = new ArrayDeque<>();
		progressBar.setMaximum(reader.getNumImages(true));
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
					progressBar.setValue(progressBar.getValue() + 1);
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
		out.close();
		progressBar.setValue(progressBar.getMaximum());
	}
}
