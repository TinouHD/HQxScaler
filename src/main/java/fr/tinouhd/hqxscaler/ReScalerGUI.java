package fr.tinouhd.hqxscaler;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;

final class ReScalerGUI
{
	public JPanel mainPanel;
	private JButton processButton;
	private JButton selectFileButton;
	private JTextField fileInput;
	private JLabel fileInputLabel;
	private JPanel buttonPanel;
	private JComboBox<Integer> sizeSelector;
	private JProgressBar progressBar;

	public ReScalerGUI()
	{
		sizeSelector.addItem(2);
		sizeSelector.addItem(3);
		sizeSelector.addItem(4);

		selectFileButton.addActionListener(e -> {
			JFileChooser fileChooser = new JFileChooser();
			FileFilter imagesFilter = new FileNameExtensionFilter("Images", "bmp", "gif", "jpg", "jpeg", "png");
			FileFilter videoFilter = new FileNameExtensionFilter("Video", "mp4");
			fileChooser.addChoosableFileFilter(imagesFilter);
			fileChooser.addChoosableFileFilter(videoFilter);

			int choice = fileChooser.showOpenDialog(null);

			if (choice != JFileChooser.APPROVE_OPTION)
				return;

			fileInput.setText(fileChooser.getSelectedFile().getAbsolutePath());
		});

		processButton.addActionListener(e -> {
			if (fileInput.getText().isEmpty() || fileInput.getText() == null || sizeSelector.getSelectedItem() == null || (int) sizeSelector.getSelectedItem() < 2 || (int) sizeSelector.getSelectedItem() > 4)
				return;
			File f = new File(fileInput.getText());
			int scale = (int) sizeSelector.getSelectedItem();
			if (!f.exists())
				return;

			Thread t = new Thread(() -> {
				processButton.setEnabled(false);
				ReScaler rs = new ReScaler(scale);
				GuiReScaler grs = new GuiReScaler(scale, progressBar);
				progressBar.setMinimum(0);
				if(f.isDirectory())
				{
					progressBar.setMaximum(f.listFiles().length);
					for (File img : f.listFiles())
					{
						if(img.getName().matches("^.*\\.(bmp|gif|jpg|jpeg|png|mp4)$"))
						{
							rs.processFileAndSave(img);
							progressBar.setValue(progressBar.getValue() + 1);
						}
					}
				}else
				{
					if(f.getName().matches("^.*\\.(bmp|gif|jpg|jpeg|png|mp4)$"))
					{
						grs.processFileAndSave(f);
					}
				}
				rs.close();
				JOptionPane.showMessageDialog(null, "All is done !", "HQx ReScaler", JOptionPane.INFORMATION_MESSAGE);
				processButton.setEnabled(true);
				progressBar.setValue(0);
			});
			t.start();
		});
	}
}
