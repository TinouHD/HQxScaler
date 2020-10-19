package fr.tinouhd.hqxscaler;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;

public class ReScalerGUI
{
	public JPanel mainPanel;
	private JButton processButton;
	private JButton selectFileButton;
	private JTextField fileInput;
	private JLabel fileInputLabel;
	private JPanel buttonPanel;
	private JComboBox<Integer> sizeSelector;

	public ReScalerGUI()
	{
		sizeSelector.addItem(2);
		sizeSelector.addItem(3);
		sizeSelector.addItem(4);

		selectFileButton.addActionListener(e -> {
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("Images", "bmp", "gif", "jpg", "jpeg", "png"));
			fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

			int choice = fileChooser.showOpenDialog(null);

			if (choice != JFileChooser.APPROVE_OPTION)
				return;

			fileInput.setText(fileChooser.getSelectedFile().getAbsolutePath());
		});

		processButton.addActionListener(e -> {
			if (fileInput.getText().isEmpty() || fileInput.getText() == null || sizeSelector.getSelectedItem() == null || (int) sizeSelector.getSelectedItem() < 2 || (int) sizeSelector.getSelectedItem() > 4)
				return;
			File f = new File(fileInput.getText());
			if (!f.exists())
				return;

			Thread t = new Thread(() -> {
				processButton.setEnabled(false);

				processFile(f);

				JOptionPane.showMessageDialog(null, "All is done !", "HQx ReScaler", JOptionPane.INFORMATION_MESSAGE);
				processButton.setEnabled(true);
			});
			t.start();
		});
	}

	private void processFile(File f)
	{
		int scale = (int) sizeSelector.getSelectedItem();
		if(f.isDirectory())
		{
			for (File listFile : f.listFiles()) {
				processFile(listFile);
			}
		}else
		{
			ReScaler rs = new ReScaler(f, scale);
			rs.saveToFile();
		}
	}
}
