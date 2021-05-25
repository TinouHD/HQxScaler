package fr.tinouhd.hqxscaler;

import javax.swing.*;
import java.io.File;

final class Main
{
	public static void main(String[] args)
	{
		if(args.length == 0)
		{
			try
			{
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ignored)
			{
			}
			JFrame frame = new JFrame();
			frame.setContentPane(new ReScalerGUI().mainPanel);
			frame.setTitle("HQx ReScaler");
			frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
			frame.setResizable(false);
			frame.pack();
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		}else if(args.length == 1)
		{
			File f = new File(args[0]);
			if(f.exists())
			{
				ReScaler rs = new ReScaler(2, true);
				rs.processFileAndSave(f);
				rs.close();
			}else
			{
				throw new IllegalArgumentException();
			}
		}else
		{
			throw new IllegalArgumentException();
		}
	}
}
