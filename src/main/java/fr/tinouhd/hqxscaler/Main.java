package fr.tinouhd.hqxscaler;

import javax.swing.*;

public class Main
{
	public static void main(String[] args)
	{
		JFrame frame = new JFrame();
		frame.setContentPane(new ReScalerGUI().mainPanel);
		frame.setTitle("HQx ReScaler");
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setLocationRelativeTo(null);
		frame.setResizable(false);
		frame.pack();
		frame.setVisible(true);
	}
}
