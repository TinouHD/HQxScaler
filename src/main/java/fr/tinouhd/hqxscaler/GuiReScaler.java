package fr.tinouhd.hqxscaler;

import javafx.util.Pair;
import org.jcodec.api.awt.AWTFrameGrab;
import org.jcodec.api.awt.AWTSequenceEncoder;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
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

	@Override public void processFileAndSave(File f)
	{
		if(f.getName().matches("^.*\\.(bmp|gif|jpg|jpeg|png)$"))
		{
			super.processFileAndSave(f);
		}else if(f.getName().matches("^.*\\.mp4$"))
		{
			try
			{
				FileChannelWrapper ch = NIOUtils.readableChannel(f);
				MP4Demuxer demuxer = MP4Demuxer.createMP4Demuxer(ch);
				DemuxerTrack video = demuxer.getVideoTrack();
				System.out.println(video.getMeta().getTotalFrames());
				progressBar.setMaximum(video.getMeta().getTotalFrames());
				File out = new File("out", f.getName().split("\\.")[0] + ".mp4");
				AWTSequenceEncoder encoder = AWTSequenceEncoder.createSequenceEncoder(out, (int)(video.getMeta().getTotalFrames() / video.getMeta().getTotalDuration()));
				ThreadPoolExecutor collectorExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
				Queue<Future<Pair<String, BufferedImage>>> futures = new ArrayDeque<>();
				for (int i = 0; i < video.getMeta().getTotalFrames(); i++)
				{
					int finalI = i;
					collectorExecutor.submit(() -> futures.add(super.executor.submit(() -> processImage(f.getName().split("\\.")[0] + "#" + finalI, AWTFrameGrab.getFrame(f, finalI)))));
				}
				int i = 0;
				while(!futures.isEmpty() || collectorExecutor.getActiveCount() > 0)
				{
					if(futures.peek() != null){
						if(futures.peek().isDone()){
							System.out.println("Encoding " + futures.peek().get().getKey() + " |\t" + i);
							i++;
							encoder.encodeImage(futures.poll().get().getValue());
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
				progressBar.setValue(progressBar.getMaximum());
				encoder.finish();

			} catch (IOException | InterruptedException | ExecutionException e)
			{}
		}else
		{
			throw new UnsupportedOperationException();
		}
	}
}
