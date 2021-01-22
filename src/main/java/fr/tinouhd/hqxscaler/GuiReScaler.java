package fr.tinouhd.hqxscaler;

import com.zakgof.velvetvideo.*;
import com.zakgof.velvetvideo.impl.VelvetVideoLib;
import javafx.util.Pair;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayDeque;
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

	@Override public void processVideo(File video) throws InterruptedException, ExecutionException
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
					audioEncoder.encode(audioDecoder.nextFrame().samples());
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
}
