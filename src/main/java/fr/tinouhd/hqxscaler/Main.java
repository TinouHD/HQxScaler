package fr.tinouhd.hqxscaler;

import javax.swing.*;

public final class Main
{
	public static void main(String[] args)
	{
		JFrame frame = new JFrame();
		frame.setContentPane(new ReScalerGUI().mainPanel);
		frame.setTitle("HQx ReScaler");
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setResizable(false);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}
}
