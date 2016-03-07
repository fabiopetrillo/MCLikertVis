package br.ufrgs.ppgc.mdlikertvis;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;

import org.jdesktop.swingx.JXTaskPane;
import org.jdesktop.swingx.JXTaskPaneContainer;
import org.jdesktop.swingx.border.DropShadowBorder;
import org.jdesktop.swingx.plaf.LookAndFeelAddons;
import org.jdesktop.swingx.plaf.windows.WindowsLookAndFeelAddons;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StackedBarRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class Main {
	
	static enum ChartType {Bar, Stacked, Pie };

	public static JFreeChart detailChart;
	
	public static JPanel filtersPanel;

	public static JTabbedPane tabbedPane = new JTabbedPane();
	
	public static boolean addAgree = false;
	public static boolean addDisagree = false;

	public static int agreeLevel = 0;
	public static int disagreeLevel = 0;

	public static JPanel chartsPanel;
	
	public static JTable table;
	
	public static Hashtable<String, double[]> detailsTable = new Hashtable<String, double[]>();

	private static ChartType chartType = ChartType.Stacked;

	private static int modeLevel = 0;

	private static double interquartileLevel = 0;

	public static void main(String[] args) {
		
		final JFrame frame = new JFrame("Multichart Likert Scale Visualization");
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});

		try {
			UIManager.setLookAndFeel("com.jgoodies.looks.plastic.PlasticXPLookAndFeel");
			UIManager.put("win.xpstyle.name", "metallic");
			LookAndFeelAddons.setAddon(WindowsLookAndFeelAddons.class);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
		chartsPanel = buildMainPanel();
		
		JScrollPane chartPane = new JScrollPane(chartsPanel,
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

		JSplitPane mainPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
 		mainPane.setDividerLocation(500);
 		
		Object[][] rows = { { "Mode", 5.0},
				{ "1st Quartile", 4.0 },
				{ "Median (2nd Quartile)", 5.0 },
				{ "3rd Quartile", 5.0 },
				{ "Interquartile Range", 1.0 } };		


		String[] colNames = { "Index", "Value" };
		table = new JTable(rows,colNames);
		
		JFreeChart oldChart = ((ChartPanel) chartsPanel.getComponent(21)).getChart();

		CategoryDataset ds = oldChart.getCategoryPlot().getDataset();

		String title = "Dataset/Statement "
				+ ds.getColumnKey(0).toString();
		detailChart = ChartFactory.createStackedBarChart(title, "",
				"(%)", ds, PlotOrientation.VERTICAL, true, true, false);
		
		CategoryPlot categoryPlot = (CategoryPlot) detailChart.getPlot();
		categoryPlot.getDomainAxis().setTickLabelsVisible(true);
		categoryPlot.getDomainAxis().setVisible(false);
		categoryPlot.getRangeAxis().setVisible(true);

		StackedBarRenderer render = (StackedBarRenderer) categoryPlot
				.getRenderer();
		render.setDrawBarOutline(true);
		render.setBaseItemLabelsVisible(true);
		render.setBaseItemLabelGenerator(new StandardCategoryItemLabelGenerator());
	    render.setShadowVisible(true);

	    render.setSeriesPaint(0, Color.green);
	    render.setSeriesPaint(1, Color.cyan);
	    render.setSeriesPaint(2, Color.yellow);
	    render.setSeriesPaint(3, Color.orange);
	    render.setSeriesPaint(4, Color.red);
	    
		for (int i = 0; i < chartsPanel.getComponentCount(); i++) {
			if (chartsPanel.getComponent(i).getClass().getCanonicalName().contains("ChartPanel")) {
				chartsPanel.getComponent(i).addMouseListener(new DetailsMouseListener(detailChart));
			}
		}

		JPanel chartTypePanel = new JPanel();
		
		final JRadioButton stackedRadio = new JRadioButton("Stacked Bar", true);
		final JRadioButton barRadio = new JRadioButton("Simple Bar");
		final JRadioButton pieRadio = new JRadioButton("Pie");

		stackedRadio.setActionCommand(ChartType.Stacked.toString());
		barRadio.setActionCommand(ChartType.Bar.toString());
		pieRadio.setActionCommand(ChartType.Pie.toString());
		
		ButtonGroup chartTypeGroup = new ButtonGroup();
		chartTypeGroup.add(stackedRadio);
		chartTypeGroup.add(barRadio);
		chartTypeGroup.add(pieRadio);
		
		chartTypePanel.add(stackedRadio);
		chartTypePanel.add(barRadio);
		chartTypePanel.add(pieRadio);
		
		ActionListener action = new ActionListener() {
			
			public void actionPerformed(ActionEvent arg) {
				if (arg.getActionCommand().equals(ChartType.Bar.toString())) {
					chartType = ChartType.Bar;
				} else 
				if (arg.getActionCommand().equals(ChartType.Pie.toString())) {
					chartType = ChartType.Pie;
				} else {
					chartType = ChartType.Stacked;
				}
			}
		};
		
		stackedRadio.addActionListener(action);
		barRadio.addActionListener(action);
		pieRadio.addActionListener(action);
		
		
		final JCheckBox addAgreeBox = new JCheckBox("Agree Answers");
		final JCheckBox addDisagreeBox = new JCheckBox("Disagree Answers");
		
		ItemListener itemListenerCheboxes = new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				addAgree = addAgreeBox.isSelected();
				addDisagree = addDisagreeBox.isSelected();
			}
		};
		
		addAgreeBox.addItemListener(itemListenerCheboxes);
		addDisagreeBox.addItemListener(itemListenerCheboxes);

		JSlider agreeSlider = new JSlider(0, 100, 0);
		JSlider disagreeSlider = new JSlider(0, 100, 0);


		final JLabel agreeLevelLabel = new JLabel("Agree Level");
		final JLabel disagreeLevelLabel = new JLabel("Disagree Level");


		JSlider interquartileSlider = new JSlider(0, 100, 0);
		final JLabel interquartileLabel = new JLabel("Interquartile Range ");
		
		interquartileSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				JSlider slider = (JSlider) e.getSource();
				interquartileLevel = Double.valueOf(slider.getValue())/20;
				
				interquartileLabel.setText(String.format("Interquartile Range %.2f", interquartileLevel));
			}
		});
		

		JSlider modeSlider = new JSlider(0, 5, 0);
		final JLabel modeLabel = new JLabel("Mode");
		
		modeSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				JSlider slider = (JSlider) e.getSource();
				modeLevel = slider.getValue();
				
				modeLabel.setText(String.format("Mode %d", modeLevel));
			}
		});
		
		
		agreeSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				JSlider slider = (JSlider) e.getSource();
				agreeLevel = slider.getValue();
				
				agreeLevelLabel.setText(String.format("Agree Level %d%%", agreeLevel));
			}
		});
		
		disagreeSlider.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider slider = (JSlider) e.getSource();
				disagreeLevel = slider.getValue();
				
				disagreeLevelLabel.setText(String.format("Disagree Level %d%%", disagreeLevel));
			}
		});

		JButton calculateButton  = new JButton("Show View");
		calculateButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				chartsPanel = buildMainPanel();
				
				for (int i = 0; i < chartsPanel.getComponentCount(); i++) {
					if (chartsPanel.getComponent(i).getClass().getCanonicalName().contains("ChartPanel")) {
						chartsPanel.getComponent(i).addMouseListener(new DetailsMouseListener(detailChart));
					}
				}

				JScrollPane chartPane = new JScrollPane(chartsPanel,
						JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
						JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
				
				tabbedPane.addTab("View " + (tabbedPane.getTabCount() + (agreeLevel > 0 ? " - Agree > " + agreeLevel + "%" : "") + (disagreeLevel > 0 ? " - Disagree > " + disagreeLevel + "%" : "")), chartPane);
				tabbedPane.setSelectedIndex(tabbedPane.getComponentCount() - 1);
			}
		});
		
		JButton clearButton = new JButton("Clear Views");
		clearButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				for (int i = tabbedPane.getComponentCount() - 1; i > 0; i--) {
					tabbedPane.remove(i);
				}
			}
		});
			
		FormLayout layout = new FormLayout(
				"10dlu, 42dlu, 10dlu, 52dlu, 30dlu, pref:grow", // columns
				"min,2dlu,min,2dlu,min,2dlu,min,2dlu,min,2dlu,min,2dlu,min,2dlu,min,2dlu,min,2dlu,min,2dlu,min,2dlu,min,2dlu,min,2dlu,min,10dlu,min"); // rows

		CellConstraints cc = new CellConstraints();
		
		PanelBuilder builder = new PanelBuilder(layout);
		builder.setDefaultDialogBorder();
		builder.addSeparator("Aggregate Values", cc.xyw(1, 1, 6));
		builder.add(addAgreeBox, cc.xyw(2, 3, 4));
		builder.add(addDisagreeBox, cc.xyw(2, 5, 4));

		builder.addSeparator("Filters", cc.xyw(1, 7, 6));
		builder.add(agreeLevelLabel, cc.xyw(2, 9, 5, "c,b"));
		builder.add(agreeSlider, cc.xyw(2, 11, 5));

		builder.add(disagreeLevelLabel, cc.xyw(2, 13, 5, "c,b"));
		builder.add(disagreeSlider, cc.xyw(2, 15, 5));

		builder.add(modeLabel, cc.xyw(2, 17, 5, "c,b"));
		builder.add(modeSlider, cc.xyw(2, 19, 5));
		
		builder.add(interquartileLabel, cc.xyw(2, 21, 5, "c,b"));
		builder.add(interquartileSlider, cc.xyw(2, 23, 5));
		
		builder.addSeparator("Chart Type", cc.xyw(1, 25, 6));
		builder.add(chartTypePanel, cc.xyw(1,27,6,"l,b"));
		
		builder.add(calculateButton, cc.xyw(1, 29,2));
		builder.add(clearButton, cc.xy(4, 29));

		filtersPanel = builder.getPanel();
		
 		tabbedPane.addTab("Complete View", chartPane);
 		mainPane.add(tabbedPane);
 		
		JScrollPane detailsScrollPane = new JScrollPane(createDetailsArea());

 		mainPane.add(detailsScrollPane);
 		mainPane.setDividerLocation(855);
 		mainPane.setOneTouchExpandable(true);


 		JMenu fileMenu = new JMenu("File");
 		JMenuItem openMenuItem = new JMenuItem("Open project");
 		JMenuItem saveMenuItem = new JMenuItem("Save project");
 		JMenuItem importMenuItem = new JMenuItem("Import survey");
 		JMenuItem printMenuItem = new JMenuItem("Print...");
 		JMenuItem exportMenuItem = new JMenuItem("Export...");
 		JMenuItem exitMenuItem = new JMenuItem("Exit");
 		
 		exitMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent a) {
				System.exit(0);
			}
		});
 		
 		exportMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent a) {
				JFileChooser fileChooser = new JFileChooser();
				
				FileFilter filter = new FileNameExtensionFilter("PNG, JPEG or GIF File", "png", "jpg", "gif");
				
				fileChooser.setFileFilter(filter);
				fileChooser.setApproveButtonText("Export...");
				int returnVal = fileChooser.showOpenDialog(frame);
				
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					File file = fileChooser.getSelectedFile();
					String extension = file.getName().substring(file.getName().length()-3,file.getName().length()).toUpperCase();

					if (!(extension.equals("PNG") || extension.equals("JPG") || extension.equals("GIF"))) {
						extension = "PNG";
					}
					
					try {
						savePanel(chartsPanel, file, extension);
						JOptionPane.showMessageDialog(frame, "Image " + file.getName() + " successfully exported!");
					} catch (Exception e) {
						e.printStackTrace();
						JOptionPane.showMessageDialog(frame, "Failed to export " + file.getName() + " image!");
					}
				}
			}
		});
 		
 		printMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent a) {
				PrintUtil.printComponent(chartsPanel);
			}
		});

 		
 		fileMenu.add(openMenuItem);
 		fileMenu.add(saveMenuItem);
 		fileMenu.add(importMenuItem);
 		fileMenu.add(exportMenuItem);
 		fileMenu.add(printMenuItem);
 		fileMenu.addSeparator();
 		fileMenu.add(exitMenuItem);
 		
 		JMenu helpMenu = new JMenu("Help");
 		JMenuItem aboutMenuItem = new JMenuItem("About");
 		helpMenu.add(aboutMenuItem);
 		
 		JMenuBar mainMenuBar = new JMenuBar();
 		mainMenuBar.add(fileMenu);
 		mainMenuBar.add(helpMenu);
 		
 		frame.setJMenuBar(mainMenuBar);
 		
 		frame.getContentPane().add(mainPane, BorderLayout.CENTER);
		//frame.setSize(1680, 1050);
		frame.setSize(1280, 800);
		
		frame.setVisible(true);
	}
	
	private static JPanel createDetailsArea() {
		JXTaskPaneContainer taskPaneContainer = new JXTaskPaneContainer();
		taskPaneContainer.setBorder(new DropShadowBorder());
		taskPaneContainer.setBackground(Color.LIGHT_GRAY);
		
		JXTaskPane tpChart = new JXTaskPane();
		//tpChart.setBorder(BorderFactory.createEmptyBorder());
		JXTaskPane tpTable = new JXTaskPane();
		JXTaskPane tpFilter = new JXTaskPane();
		
		tpChart.setTitle("Detailed Chart");
		tpTable.setTitle("Statistical Informations");
		tpFilter.setTitle("Options");
		
		JPanel chartPanel = new ChartPanel(detailChart, 100, 400,0,0,1000,1400,true,true,true,true,true,true,true);
		chartPanel.setBorder(BorderFactory.createEmptyBorder());
		//JPanel chartPanel = new ChartPanel(detailChart);
		
		tpChart.add(chartPanel);
		tpTable.add(table);
		tpFilter.add(filtersPanel);
		
		taskPaneContainer.add(tpChart);
		taskPaneContainer.add(tpTable);
		taskPaneContainer.add(tpFilter);
		
		return taskPaneContainer;
	}

	private static void savePanel(JPanel panel, File file, String format) throws Exception {
		 BufferedImage image = createImage(panel);
		 try {
			ImageIO.write(image, format, file);
		} catch (IOException e) {
			throw new Exception("Failed to save the file " + file.getName());
		}
	}
	
	private static JPanel buildMainPanel() {
		FormLayout layout = new FormLayout(
				"10dlu,min, 3dlu, 63dlu, 1dlu, 63dlu,1dlu, 63dlu,1dlu, 63dlu,1dlu, 63dlu,1dlu, 63dlu,1dlu, 63dlu, min", // columns
				"20dlu,10dlu, 2dlu, 50dlu, 2dlu, 50dlu, 2dlu, 50dlu, 2dlu, 50dlu, 2dlu, 50dlu, 2dlu, 50dlu, 2dlu, 50dlu, 2dlu, 50dlu, 2dlu, 50dlu, 2dlu, 50dlu, 2dlu, 50dlu"); // rows

		JPanel panel = new JPanel(layout);

		JLabel labelS1 = new JLabel("S1");
		labelS1
				.setToolTipText("É fácil de reconhecer clusters (grupos de nodos fortemente conectados).");

		JLabel labelS2 = new JLabel("S2");
		labelS2
				.setToolTipText("É fácil de identificar nodos de corte (nodos que, se removidos, desconectam dois subgrafos).");

		JLabel labelS3 = new JLabel("S3");
		labelS3
				.setToolTipText("É fácil de identificar folhas (nodos com só um nodo adjacente).");

		JLabel labelS4 = new JLabel("S4");
		labelS4
				.setToolTipText("É fácil perceber o caminho que liga dois nodos.");

		JLabel labelS5 = new JLabel("S5");
		labelS5.setToolTipText("Em geral, o layout é claro.");

		JLabel labelS6 = new JLabel("S6");
		labelS6.setToolTipText("O layout é mais claro que o layout original.");

		JLabel labelS7 = new JLabel("S7");
		labelS7
				.setToolTipText("O layout representa adequadamente a informação que ele se propõe a evidenciar (ver informação de descrição dos layouts).");

		JLabel affirmationLabel = new JLabel("Statements");
		Font font = new Font(affirmationLabel.getFont().getName(),Font.BOLD,20);
		affirmationLabel.setFont(font);		


		JLabel layoutLabel = new JLabel("<HTML>D<br>a<br>t<br>a<br>s<br>e<br>t<br>s</HTML>");
		layoutLabel.setFont(font);

		CellConstraints cc = new CellConstraints();
		
		panel.add(affirmationLabel, cc.xyw(2, 1,16,"c,c"));
		panel.add(layoutLabel, cc.xywh(1, 1,1,18,"c,c"));
		panel.add(new JLabel("D/S"), cc.xy(2, 2));
		
		panel.add(labelS1, cc.xy(4, 2, "c,c"));
		panel.add(labelS2, cc.xy(6, 2, "c,c"));
		panel.add(labelS3, cc.xy(8, 2, "c,c"));
		panel.add(labelS4, cc.xy(10, 2, "c,c"));
		panel.add(labelS5, cc.xy(12, 2, "c,c"));
		panel.add(labelS6, cc.xy(14, 2, "c,c"));
		panel.add(labelS7, cc.xy(16, 2, "c,c"));

		panel.add(new JLabel("D01"), cc.xy(2, 4));
		panel.add(new JLabel("D02"), cc.xy(2, 6));
		panel.add(new JLabel("D03"), cc.xy(2, 8));
		panel.add(new JLabel("D04"), cc.xy(2, 10));
		panel.add(new JLabel("D05"), cc.xy(2, 12));
		panel.add(new JLabel("D06"), cc.xy(2, 14));
		panel.add(new JLabel("D07"), cc.xy(2, 16));
		panel.add(new JLabel("D08"), cc.xy(2, 18));
		panel.add(new JLabel("D09"), cc.xy(2, 20));
		panel.add(new JLabel("D10"), cc.xy(2, 22));
		panel.add(new JLabel("D11"), cc.xy(2, 24));

		buildG1(panel, cc);
		buildG2(panel, cc);
		buildG3(panel, cc);
		buildG4(panel, cc);
		buildG5(panel, cc);
		buildG6(panel, cc);
		buildG7(panel, cc);
		buildG8(panel, cc);
		buildG9(panel, cc);
		buildG10(panel, cc);
		buildG11(panel, cc);
		return panel;
	}

	private static DefaultCategoryDataset getCategoryDatase(double[] values,
			String label) {
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		if(addAgree) {
			dataset.addValue(values[0]+values[1], "Agree", label);
		} else {
			dataset.addValue(values[0], "Strongly Agree", label);
			dataset.addValue(values[1], "Agree", label);
		}
			
		dataset.addValue(values[2], "Undecided", label);

		if(addDisagree) {
			dataset.addValue(values[3]+values[4], "Disagree", label);
		} else {
			dataset.addValue(values[3], "Disagree", label);
			dataset.addValue(values[4], "Strongly Disagree", label);
		}
		
		return dataset;
	}
	
	private static JPanel createStackedChart(double[] sample, double[] details, String label) {
		DefaultCategoryDataset ds = getCategoryDatase(sample, label);

		JFreeChart chart = ChartFactory.createStackedBarChart("", "", "", ds,
				PlotOrientation.VERTICAL, false, true, false);
		CategoryPlot categoryPlot = (CategoryPlot) chart.getPlot();
		StackedBarRenderer render = (StackedBarRenderer) categoryPlot
				.getRenderer();
		render.setDrawBarOutline(true);
		render.setBaseItemLabelsVisible(false);
		render.setBaseItemLabelGenerator(new StandardCategoryItemLabelGenerator());
		
		if (addAgree && addDisagree) {
		    render.setSeriesPaint(0, Color.green);
		    render.setSeriesPaint(1, Color.yellow);
		    render.setSeriesPaint(2, Color.red);
		} else
		if (addAgree && !addDisagree) {
		    render.setSeriesPaint(0, Color.green);
		    render.setSeriesPaint(1, Color.yellow);
		    render.setSeriesPaint(2, Color.orange);
		    render.setSeriesPaint(3, Color.red);
		} else		
		if (!addAgree && addDisagree) {
		    render.setSeriesPaint(0, Color.green);
		    render.setSeriesPaint(1, Color.cyan);
		    render.setSeriesPaint(2, Color.yellow);
		    render.setSeriesPaint(3, Color.red);
		} 	else {
		    render.setSeriesPaint(0, Color.green);
		    render.setSeriesPaint(1, Color.cyan);
		    render.setSeriesPaint(2, Color.yellow);
		    render.setSeriesPaint(3, Color.orange);
		    render.setSeriesPaint(4, Color.red);
		}
		
	    render.setShadowVisible(false);

		categoryPlot.getDomainAxis().setTickLabelsVisible(false);
		categoryPlot.getDomainAxis().setVisible(false);
		categoryPlot.getRangeAxis().setVisible(false);
		chart.setBorderVisible(true);
		
		ChartPanel panel = new ChartPanel(chart);

		applyFilters(sample, details, panel);
		
		return panel;
	}

	
	private static void applyFilters(double[] sample, double[] details,
			ChartPanel panel) {
		if (agreeLevel > 0) {
			if (sample[0]+sample[1] < agreeLevel) {
				panel.setVisible(false);
			}
		}

		if (disagreeLevel > 0) {
			if (sample[3]+sample[4] < disagreeLevel) {
				panel.setVisible(false);
			}
		}
		
		if (modeLevel > 0) {
			if (modeLevel >= details[0]) {
				panel.setVisible(false);
			}
		}

		if (interquartileLevel > 0) {
			if (interquartileLevel > details[4]) {
				panel.setVisible(false);
			}
		}
	}

	private static JPanel createBarChart(double[] sample, double[] details, String label) {
		DefaultCategoryDataset ds = getCategoryDatase(sample, label);

		JFreeChart chart = ChartFactory.createBarChart("", "", "", ds,
				PlotOrientation.VERTICAL, false, true, false);
		CategoryPlot categoryPlot = (CategoryPlot) chart.getPlot();
		BarRenderer render = (BarRenderer) categoryPlot
				.getRenderer();
		render.setDrawBarOutline(true);
		render.setBaseItemLabelsVisible(false);
		render.setBaseItemLabelGenerator(new StandardCategoryItemLabelGenerator());
		
		if (addAgree && addDisagree) {
		    render.setSeriesPaint(0, Color.green);
		    render.setSeriesPaint(1, Color.yellow);
		    render.setSeriesPaint(2, Color.red);
		} else
		if (addAgree && !addDisagree) {
		    render.setSeriesPaint(0, Color.green);
		    render.setSeriesPaint(1, Color.yellow);
		    render.setSeriesPaint(2, Color.orange);
		    render.setSeriesPaint(3, Color.red);
		} else		
		if (!addAgree && addDisagree) {
		    render.setSeriesPaint(0, Color.green);
		    render.setSeriesPaint(1, Color.cyan);
		    render.setSeriesPaint(2, Color.yellow);
		    render.setSeriesPaint(3, Color.red);
		} 	else {
		    render.setSeriesPaint(0, Color.green);
		    render.setSeriesPaint(1, Color.cyan);
		    render.setSeriesPaint(2, Color.yellow);
		    render.setSeriesPaint(3, Color.orange);
		    render.setSeriesPaint(4, Color.red);
		}
		
	    render.setShadowVisible(false);

		categoryPlot.getDomainAxis().setTickLabelsVisible(false);
		categoryPlot.getDomainAxis().setVisible(false);
		categoryPlot.getRangeAxis().setVisible(false);
		chart.setBorderVisible(true);
		
		ChartPanel panel = new ChartPanel(chart);
		
		applyFilters(sample, details, panel);
		
		return panel;
	}
	

	private static JPanel createPieChart(double[] sample, double[] details, String label) {
		DefaultPieDataset dataset = new DefaultPieDataset();
		
		if(addAgree) {
			dataset.setValue("Agree", sample[0]+sample[1]);
		} else {
			dataset.setValue("Strongly Agree", sample[0]);
			dataset.setValue("Agree", sample[1]);
		}
			
		dataset.setValue("Undecided", sample[2]);

		if(addDisagree) {
			dataset.setValue("Disagree", sample[3]+sample[4]);
		} else {
			dataset.setValue("Disagree", sample[3]);
			dataset.setValue("Strongly Disagree", sample[4]);
		}

		JFreeChart chart = ChartFactory.createPieChart("", dataset,false,true,false);
		
		PiePlot pp = (PiePlot) chart.getPlot();
		pp.setCircular(true);
		pp.setLabelLinksVisible(false);
		pp.setLabelGenerator(null);
		
		if (addAgree && addDisagree) {
		    pp.setSectionPaint("Agree", Color.green);
		    pp.setSectionPaint("Undecided", Color.yellow);
		    pp.setSectionPaint("Disagree", Color.red);
		} else
		if (addAgree && !addDisagree) {
		    pp.setSectionPaint("Agree", Color.green);
		    pp.setSectionPaint("Undecided", Color.yellow);
		    pp.setSectionPaint("Disagree", Color.orange);
		    pp.setSectionPaint("Strongly Disagree", Color.red);
		} else		
		if (!addAgree && addDisagree) {
		    pp.setSectionPaint("Strongly Agree", Color.green);
		    pp.setSectionPaint("Agree", Color.cyan);
		    pp.setSectionPaint("Undecided", Color.yellow);
		    pp.setSectionPaint("Disagree", Color.orange);
		} 	else {
		    pp.setSectionPaint("Strongly Agree", Color.green);
		    pp.setSectionPaint("Agree", Color.cyan);
		    pp.setSectionPaint("Undecided", Color.yellow);
		    pp.setSectionPaint("Disagree", Color.orange);
		    pp.setSectionPaint("Strongly Disagree", Color.red);
		}
		
		chart.setBorderVisible(true);
		
		ChartPanel panel = new ChartPanel(chart);
		applyFilters(sample, details, panel);
		
		return panel;
	}
	
	

	final static public BufferedImage createImage(JPanel panel) {
	    int w = panel.getWidth();
	    int h = panel.getHeight();
	    BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
	    Graphics2D g = bi.createGraphics();
	    panel.paint(g);
	    return bi;
	}


	private static void createSample(List<double[]> sample, String label,
			JPanel panel, CellConstraints cc, int line, List<double[]> details) {
		int position[] = { 4, 6, 8, 10, 12, 14, 16 };
		for (int i = 0; i < 7; i++) {
			String name = label + "/S" + (i + 1);
			
			JPanel chart;
			
			if (chartType == ChartType.Bar) {
				chart = createBarChart(sample.get(i), details.get(i), name);
			} else  
			if (chartType == ChartType.Pie) {
				chart = createPieChart(sample.get(i), details.get(i), name);
			} else {
				chart = createStackedChart(sample.get(i), details.get(i), name);
			}
			
			panel.add(chart, cc.xy(position[i], line));
		}
	}

	private static void buildG1(JPanel panel, CellConstraints cc) {
		double[] sampleS1 = { 57.14, 21.43, 7.14, 7.14, 7.14, 0 };
		double[] sampleS2 = { 35.71, 35.71, 0.00, 21.43, 7.14, 0.00 };
		double[] sampleS3 = { 57.14, 28.57, 0.00, 7.14, 7.14, 0.00 };
		double[] sampleS4 = { 21.43, 42.86, 28.57, 0.00, 7.14, 0.00 };
		double[] sampleS5 = { 42.86, 42.86, 7.14, 0.00, 7.14, 0.00 };
		double[] sampleS6 = { 0, 0, 0.00, 0, 0, 0.00 };
		double[] sampleS7 = { 0, 0, 0.00, 0, 0, 0.00 };

		List<double[]> sample = new ArrayList<double[]>();
		sample.add(sampleS1);
		sample.add(sampleS2);
		sample.add(sampleS3);
		sample.add(sampleS4);
		sample.add(sampleS5);
		sample.add(sampleS6);
		sample.add(sampleS7);

		double[] detailS1 = { 5, 4, 5, 5, 1 };
		double[] detailS2 = { 4, 2.5, 4, 5, 2.5 };
		double[] detailS3 = { 5, 4, 5, 5, 1 };
		double[] detailS4 = { 4, 3, 4, 4, 1 };
		double[] detailS5 = { 4, 4, 4, 5, 1 };
		double[] detailS6 = { 0, 0, 0, 0, 0 };
		double[] detailS7 = { 0, 0, 0, 0, 0 };

		List<double[]> details = new ArrayList<double[]>();
		details.add(detailS1);
		details.add(detailS2);
		details.add(detailS3);
		details.add(detailS4);
		details.add(detailS5);
		details.add(detailS6);
		details.add(detailS7);
		
		for (int i = 0; i < details.size(); i++) {
			detailsTable.put("D01/S"+(i+1) , details.get(i));
		}
			
		createSample(sample, "D01", panel, cc, 4, details);
	}

	private static void buildG2(JPanel panel, CellConstraints cc) {
		double[] sampleS1 = { 50.00, 42.86, 7.14, 0.00, 0.00, 0.00 };
		double[] sampleS2 = { 28.57, 50.00, 21.43, 0.00, 0.00, 0.00 };
		double[] sampleS3 = { 42.86, 42.86, 7.14, 7.14, 0.00, 0.00 };
		double[] sampleS4 = { 21.43, 50.00, 21.43, 0.00, 7.14, 0.00 };
		double[] sampleS5 = { 64.29, 28.57, 0.00, 0.00, 7.14, 0.00 };
		double[] sampleS6 = { 42.86, 28.57, 7.14, 7.14, 14.29, 0.00 };
		double[] sampleS7 = { 57.14, 28.57, 14.29, 0.00, 0.00, 0.00 };

		List<double[]> sample = new ArrayList<double[]>();
		sample.add(sampleS1);
		sample.add(sampleS2);
		sample.add(sampleS3);
		sample.add(sampleS4);
		sample.add(sampleS5);
		sample.add(sampleS6);
		sample.add(sampleS7);

		double[] detailS1 = { 5, 4, 4.5, 5, 1 };
		double[] detailS2 = { 4, 4, 4, 4.75, 0.75 };
		double[] detailS3 = { 4, 4, 4, 5, 1 };
		double[] detailS4 = { 4, 3.25, 4, 4, 0.75 };
		double[] detailS5 = { 5, 4, 5, 5, 1 };
		double[] detailS6 = { 5, 3.25, 4, 5, 1.75 };
		double[] detailS7 = { 5, 4, 5, 5, 1 };

		List<double[]> details = new ArrayList<double[]>();
		details.add(detailS1);
		details.add(detailS2);
		details.add(detailS3);
		details.add(detailS4);
		details.add(detailS5);
		details.add(detailS6);
		details.add(detailS7);

		for (int i = 0; i < details.size(); i++) {
			detailsTable.put("D02/S"+(i+1) , details.get(i));
		}

		createSample(sample, "D02", panel, cc, 6, details);
	}

	private static void buildG3(JPanel panel, CellConstraints cc) {
		double[] sampleS1 = { 50.00, 35.71, 14.29, 0.00, 0.00, 0.00 };
		double[] sampleS2 = { 35.71, 42.86, 21.43, 0.00, 0.00, 0.00 };
		double[] sampleS3 = { 42.86, 35.71, 21.43, 0.00, 0.00, 0.00 };
		double[] sampleS4 = { 21.43, 42.86, 35.71, 0.00, 0.00, 0.00 };
		double[] sampleS5 = { 35.71, 42.86, 21.43, 0.00, 0.00, 0.00 };
		double[] sampleS6 = { 28.57, 35.71, 14.29, 7.14, 14.29, 0.00 };
		double[] sampleS7 = { 64.29, 14.29, 21.43, 0.00, 0.00, 0.00 };

		List<double[]> sample = new ArrayList<double[]>();
		sample.add(sampleS1);
		sample.add(sampleS2);
		sample.add(sampleS3);
		sample.add(sampleS4);
		sample.add(sampleS5);
		sample.add(sampleS6);
		sample.add(sampleS7);
		
		double[] detailS1 = { 5,	4,	4.5,	5,	1 };
		double[] detailS2 = { 4,	4,	4,	5,	1 };
		double[] detailS3 = { 5,	4,	4,	5,	1 };
		double[] detailS4 = { 4,	3,	4,	4,	1 };
		double[] detailS5 = { 4,	4,	4,	5,	1 };
		double[] detailS6 = { 4,	3,	4,	4.75,	1.75 };
		double[] detailS7 = { 5,	4,	5,	5,	1};

		List<double[]> details = new ArrayList<double[]>();
		details.add(detailS1);
		details.add(detailS2);
		details.add(detailS3);
		details.add(detailS4);
		details.add(detailS5);
		details.add(detailS6);
		details.add(detailS7);		

		for (int i = 0; i < details.size(); i++) {
			detailsTable.put("D03/S"+(i+1) , details.get(i));
		}
		
		createSample(sample, "D03", panel, cc, 8, details);
	}

	private static void buildG4(JPanel panel, CellConstraints cc) {
		double[] sampleS1 = { 57.14, 42.86, 0.00, 0.00, 0.00, 0.00 };
		double[] sampleS2 = { 28.57, 50.00, 21.43, 0.00, 0.00, 0.00 };
		double[] sampleS3 = { 42.86, 35.71, 21.43, 0.00, 0.00, 0.00 };
		double[] sampleS4 = { 14.29, 50.00, 35.71, 0.00, 0.00, 0.00 };
		double[] sampleS5 = { 35.71, 64.29, 0.00, 0.00, 0.00, 0.00 };
		double[] sampleS6 = { 21.43, 42.86, 21.43, 7.14, 7.14, 0.00 };
		double[] sampleS7 = { 50.00, 42.86, 7.14, 0.00, 0.00, 0.00 };

		List<double[]> sample = new ArrayList<double[]>();
		sample.add(sampleS1);
		sample.add(sampleS2);
		sample.add(sampleS3);
		sample.add(sampleS4);
		sample.add(sampleS5);
		sample.add(sampleS6);
		sample.add(sampleS7);

		double[] detailS1 = { 5,	4,	5,	5,	1 };
		double[] detailS2 = { 4,	4,	4,	4.75,	0.75 };
		double[] detailS3 = { 5,	4,	4,	5,	1 };
		double[] detailS4 = { 4,	3,	4,	4,	1 };
		double[] detailS5 = { 4,	4,	4,	5,	1 };
		double[] detailS6 = { 4,	3,	4,	4,	1 };
		double[] detailS7 = { 5,	4,	4.5,	5,	1};

		List<double[]> details = new ArrayList<double[]>();
		details.add(detailS1);
		details.add(detailS2);
		details.add(detailS3);
		details.add(detailS4);
		details.add(detailS5);
		details.add(detailS6);
		details.add(detailS7);		

		for (int i = 0; i < details.size(); i++) {
			detailsTable.put("D04/S"+(i+1) , details.get(i));
		}
		
		createSample(sample, "D04", panel, cc, 10, details);
	}

	private static void buildG5(JPanel panel, CellConstraints cc) {
		double[] sampleS1 = { 42.86, 50.00, 0.00, 0.00, 7.14, 0.00 };
		double[] sampleS2 = { 14.29, 42.86, 35.71, 7.14, 0.00, 0.00 };
		double[] sampleS3 = { 35.71, 35.71, 21.43, 7.14, 0.00, 0.00 };
		double[] sampleS4 = { 7.14, 57.14, 21.43, 14.29, 0.00, 0.00 };
		double[] sampleS5 = { 7.14, 50.00, 35.71, 0.00, 7.14, 0.00 };
		double[] sampleS6 = { 14.29, 14.29, 28.57, 21.43, 21.43, 0.00 };
		double[] sampleS7 = { 35.71, 14.29, 21.43, 14.29, 14.29, 0.00 };

		List<double[]> sample = new ArrayList<double[]>();
		sample.add(sampleS1);
		sample.add(sampleS2);
		sample.add(sampleS3);
		sample.add(sampleS4);
		sample.add(sampleS5);
		sample.add(sampleS6);
		sample.add(sampleS7);
		
		double[] detailS1 = { 4,	4,	4,	5,	1 };
		double[] detailS2 = { 4,	3,	4,	4,	1 };
		double[] detailS3 = { 4,	3.25,	4,	5,	1.75 };
		double[] detailS4 = { 4,	3,	4,	4,	1 };
		double[] detailS5 = { 4,	3,	4,	4,	1 };
		double[] detailS6 = { 3,	2,	3,	3.75,	1.75 };
		double[] detailS7 = { 5,	2.25,	3.5,	5,	2.75};

		List<double[]> details = new ArrayList<double[]>();
		details.add(detailS1);
		details.add(detailS2);
		details.add(detailS3);
		details.add(detailS4);
		details.add(detailS5);
		details.add(detailS6);
		details.add(detailS7);		

		for (int i = 0; i < details.size(); i++) {
			detailsTable.put("D05/S"+(i+1) , details.get(i));
		}

		createSample(sample, "D05", panel, cc, 12, details);
	}

	private static void buildG6(JPanel panel, CellConstraints cc) {
		double[] sampleS1 = { 42.86, 50.00, 7.14, 0.00, 0.00, 0.00 };
		double[] sampleS2 = { 21.43, 57.14, 7.14, 14.29, 0.00, 0.00 };
		double[] sampleS3 = { 42.86, 42.86, 7.14, 7.14, 0.00, 0.00 };
		double[] sampleS4 = { 21.43, 35.71, 42.86, 0.00, 0.00, 0.00 };
		double[] sampleS5 = { 28.57, 50.00, 21.43, 0.00, 0.00, 0.00 };
		double[] sampleS6 = { 28.57, 14.29, 35.71, 7.14, 14.29, 0.00 };
		double[] sampleS7 = { 35.71, 21.43, 14.29, 21.43, 7.14, 0.00 };

		List<double[]> sample = new ArrayList<double[]>();
		sample.add(sampleS1);
		sample.add(sampleS2);
		sample.add(sampleS3);
		sample.add(sampleS4);
		sample.add(sampleS5);
		sample.add(sampleS6);
		sample.add(sampleS7);

		double[] detailS1 = { 4,	4,	4,	5,	1 };
		double[] detailS2 = { 4,	4,	4,	4,	0 };
		double[] detailS3 = { 4,	4,	4,	5,	1 };
		double[] detailS4 = { 3,	3,	4,	4,	1 };
		double[] detailS5 = { 4,	4,	4,	4.75,	0.75 };
		double[] detailS6 = { 3,	3,	3,	4.75,	1.75 };
		double[] detailS7 = { 5,	2.25,	4,	5,	2.75 };

		List<double[]> details = new ArrayList<double[]>();
		details.add(detailS1);
		details.add(detailS2);
		details.add(detailS3);
		details.add(detailS4);
		details.add(detailS5);
		details.add(detailS6);
		details.add(detailS7);		
		
		for (int i = 0; i < details.size(); i++) {
			detailsTable.put("D06/S"+(i+1) , details.get(i));
		}

		createSample(sample, "D06", panel, cc, 14, details);
	}

	private static void buildG7(JPanel panel, CellConstraints cc) {
		double[] sampleS1 = { 21.43, 28.57, 21.43, 28.57, 0.00, 0.00 };
		double[] sampleS2 = { 7.14, 14.29, 42.86, 21.43, 14.29, 0.00 };
		double[] sampleS3 = { 21.43, 21.43, 28.57, 28.57, 0.00, 0.00 };
		double[] sampleS4 = { 7.14, 28.57, 35.71, 28.57, 0.00, 0.00 };
		double[] sampleS5 = { 7.14, 50.00, 21.43, 21.43, 0.00, 0.00 };
		double[] sampleS6 = { 14.29, 14.29, 14.29, 21.43, 35.71, 0.00 };
		double[] sampleS7 = { 50.00, 21.43, 21.43, 7.14, 0.00, 0.00 };

		List<double[]> sample = new ArrayList<double[]>();
		sample.add(sampleS1);
		sample.add(sampleS2);
		sample.add(sampleS3);
		sample.add(sampleS4);
		sample.add(sampleS5);
		sample.add(sampleS6);
		sample.add(sampleS7);
		
		double[] detailS1 = { 2,	2.25,	3.5,	4,	1.75 };
		double[] detailS2 = { 3,	2,	3,	3,	1 };
		double[] detailS3 = { 2,	2.25,	3,	4,	1.75 };
		double[] detailS4 = { 3,	2.25,	3,	4,	1.75 };
		double[] detailS5 = { 4,	3,	4,	4,	1 };
		double[] detailS6 = { 1,	1,	2,	3.75,	2.75 };
		double[] detailS7 = { 5,	3.25,	4.5,	5,	1.75 };

		List<double[]> details = new ArrayList<double[]>();
		details.add(detailS1);
		details.add(detailS2);
		details.add(detailS3);
		details.add(detailS4);
		details.add(detailS5);
		details.add(detailS6);
		details.add(detailS7);		

		for (int i = 0; i < details.size(); i++) {
			detailsTable.put("D07/S"+(i+1) , details.get(i));
		}

		createSample(sample, "D07", panel, cc, 16, details);
	}

	private static void buildG8(JPanel panel, CellConstraints cc) {
		double[] sampleS1 = { 57.14, 35.71, 7.14, 0.00, 0.00, 0.00 };
		double[] sampleS2 = { 14.29, 42.86, 35.71, 7.14, 0.00, 0.00 };
		double[] sampleS3 = { 42.86, 35.71, 21.43, 0.00, 0.00, 0.00 };
		double[] sampleS4 = { 7.14, 57.14, 35.71, 0.00, 0.00, 0.00 };
		double[] sampleS5 = { 42.86, 35.71, 21.43, 0.00, 0.00, 0.00 };
		double[] sampleS6 = { 35.71, 14.29, 14.29, 28.57, 7.14, 0.00 };
		double[] sampleS7 = { 42.86, 21.43, 7.14, 28.57, 0.00, 0.00 };

		List<double[]> sample = new ArrayList<double[]>();
		sample.add(sampleS1);
		sample.add(sampleS2);
		sample.add(sampleS3);
		sample.add(sampleS4);
		sample.add(sampleS5);
		sample.add(sampleS6);
		sample.add(sampleS7);

		double[] detailS1 = { 5,	4,	5,	5,	1 };
		double[] detailS2 = { 4,	3,	4,	4,	1 };
		double[] detailS3 = { 5,	4,	4,	5,	1 };
		double[] detailS4 = { 4,	3,	4,	4,	1 };
		double[] detailS5 = { 5,	4,	4,	5,	1 };
		double[] detailS6 = { 5,	2,	3.5,	5,	3 };
		double[] detailS7 = { 5,	2.25,	4,	5,	2.75 };

		List<double[]> details = new ArrayList<double[]>();
		details.add(detailS1);
		details.add(detailS2);
		details.add(detailS3);
		details.add(detailS4);
		details.add(detailS5);
		details.add(detailS6);
		details.add(detailS7);		
		
		for (int i = 0; i < details.size(); i++) {
			detailsTable.put("D08/S"+(i+1) , details.get(i));
		}

		createSample(sample, "D08", panel, cc, 18,details);
	}

	private static void buildG9(JPanel panel, CellConstraints cc) {
		double[] sampleS1 = { 35.71, 42.86, 21.43, 0.00, 0.00, 0.00 };
		double[] sampleS2 = { 14.29, 50.00, 28.57, 7.14, 0.00, 0.00 };
		double[] sampleS3 = { 50.00, 28.57, 21.43, 0.00, 0.00, 0.00 };
		double[] sampleS4 = { 7.14, 50.00, 35.71, 7.14, 0.00, 0.00 };
		double[] sampleS5 = { 28.57, 28.57, 35.71, 7.14, 0.00, 0.00 };
		double[] sampleS6 = { 35.71, 14.29, 14.29, 28.57, 7.14, 0.00 };
		double[] sampleS7 = { 50.00, 28.57, 0.00, 14.29, 7.14, 0.00 };

		List<double[]> sample = new ArrayList<double[]>();
		sample.add(sampleS1);
		sample.add(sampleS2);
		sample.add(sampleS3);
		sample.add(sampleS4);
		sample.add(sampleS5);
		sample.add(sampleS6);
		sample.add(sampleS7);

		double[] detailS1 = { 4,	4,	4,	5,	1 };
		double[] detailS2 = { 4,	3,	4,	4,	1 };
		double[] detailS3 = { 5,	4,	4.5,	5,	1 };
		double[] detailS4 = {4,	3,	4,	4,	1};
		double[] detailS5 = { 3,	3,	4,	4.75,	1.75 };
		double[] detailS6 = { 2,	2,	3,	4.75,	2.75 };
		double[] detailS7 = { 5,	4,	4.5,	5,	1 };

		List<double[]> details = new ArrayList<double[]>();
		details.add(detailS1);
		details.add(detailS2);
		details.add(detailS3);
		details.add(detailS4);
		details.add(detailS5);
		details.add(detailS6);
		details.add(detailS7);
		
		for (int i = 0; i < details.size(); i++) {
			detailsTable.put("D09/S"+(i+1) , details.get(i));
		}

		createSample(sample, "D09", panel, cc, 20, details);
	}

	private static void buildG10(JPanel panel, CellConstraints cc) {
		double[] sampleS1 = { 42.86, 57.14, 0.00, 0.00, 0.00, 0.00 };
		double[] sampleS2 = { 14.29, 64.29, 14.29, 7.14, 0.00, 0.00 };
		double[] sampleS3 = { 28.57, 50.00, 21.43, 0.00, 0.00, 0.00 };
		double[] sampleS4 = { 14.29, 50.00, 28.57, 7.14, 0.00, 0.00 };
		double[] sampleS5 = { 28.57, 42.86, 21.43, 0.00, 7.14, 0.00 };
		double[] sampleS6 = { 28.57, 28.57, 14.29, 21.43, 7.14, 0.00 };
		double[] sampleS7 = { 50.00, 21.43, 21.43, 0.00, 7.14, 0.00 };

		List<double[]> sample = new ArrayList<double[]>();
		sample.add(sampleS1);
		sample.add(sampleS2);
		sample.add(sampleS3);
		sample.add(sampleS4);
		sample.add(sampleS5);
		sample.add(sampleS6);
		sample.add(sampleS7);

		double[] detailS1 = { 4,	4,	4,	5,	1};
		double[] detailS2 = { 4,	4,	4,	4,	0 };
		double[] detailS3 = { 4,	4,	4,	4.75,	0.75};
		double[] detailS4 = {4,	3,	4,	4,	1};
		double[] detailS5 = { 4,	3.25,	4,	4.75,	1.5 };
		double[] detailS6 = { 4,	2.25,	4,	4.75,	2.5 };
		double[] detailS7 = { 5,	3.25,	4.5,	5,	1.75 };

		List<double[]> details = new ArrayList<double[]>();
		details.add(detailS1);
		details.add(detailS2);
		details.add(detailS3);
		details.add(detailS4);
		details.add(detailS5);
		details.add(detailS6);
		details.add(detailS7);		

		for (int i = 0; i < details.size(); i++) {
			detailsTable.put("D10/S"+(i+1) , details.get(i));
		}

		createSample(sample, "D10", panel, cc, 22, details);
	}

	private static void buildG11(JPanel panel, CellConstraints cc) {
		double[] sampleS1 = { 42.86, 50.00, 7.14, 0.00, 0.00, 0.00 };
		double[] sampleS2 = { 21.43, 57.14, 14.29, 7.14, 0.00, 0.00 };
		double[] sampleS3 = { 35.71, 35.71, 28.57, 0.00, 0.00, 0.00 };
		double[] sampleS4 = { 14.29, 42.86, 35.71, 7.14, 0.00, 0.00 };
		double[] sampleS5 = { 28.57, 50.00, 14.29, 0.00, 7.14, 0.00 };
		double[] sampleS6 = { 21.43, 28.57, 14.29, 21.43, 14.29, 0.00 };
		double[] sampleS7 = { 57.14, 28.57, 7.14, 0.00, 7.14, 0.00 };

		List<double[]> sample = new ArrayList<double[]>();
		sample.add(sampleS1);
		sample.add(sampleS2);
		sample.add(sampleS3);
		sample.add(sampleS4);
		sample.add(sampleS5);
		sample.add(sampleS6);
		sample.add(sampleS7);

		double[] detailS1 = { 4,	4,	4,	5,	1};
		double[] detailS2 = { 4,	4,	4,	4,	0 };
		double[] detailS3 = { 4,	3.25,	4,	5,	1.75};
		double[] detailS4 = { 4,	3,	4,	4,	1};
		double[] detailS5 = { 4,	4,	4,	4.75,	0.75 };
		double[] detailS6 = { 4,	2,	3.5,	4,	2};
		double[] detailS7 = { 5,	4,	5,	5,	1};

		List<double[]> details = new ArrayList<double[]>();
		details.add(detailS1);
		details.add(detailS2);
		details.add(detailS3);
		details.add(detailS4);
		details.add(detailS5);
		details.add(detailS6);
		details.add(detailS7);		
		
		for (int i = 0; i < details.size(); i++) {
			detailsTable.put("D11/S"+(i+1) , details.get(i));
		}

		createSample(sample, "D11", panel, cc, 24, details);
	}
	
	static class DetailsMouseListener implements MouseListener {

		private JFreeChart detailChart;

		public DetailsMouseListener(JFreeChart detailChart) {
			this.detailChart = detailChart;
		}

		@Override
		public void mouseReleased(MouseEvent e) {

		}

		@Override
		public void mousePressed(MouseEvent e) {
			// TODO Auto-generated method stub

		}

		@Override
		public void mouseExited(MouseEvent e) {
			// TODO Auto-generated method stub

		}

		@Override
		public void mouseEntered(MouseEvent e) {
			// TODO Auto-generated method stub

		}

		@Override
		public void mouseClicked(MouseEvent e) {
			JFreeChart oldChart = ((ChartPanel) e.getComponent()).getChart();

//			if (chartType == ChartType.Bar) {
//				createBar(oldChart);
//			} else
//			if (chartType == ChartType.Pie) {
//				createPie(oldChart);
//			} else {
//				createStacked(oldChart);
//			}

			createStacked(oldChart);
			
		    table.repaint();
		}

		private void createPie(JFreeChart oldChart) {
			DefaultPieDataset ds = (DefaultPieDataset) ((PiePlot) oldChart.getPlot()).getDataset();

			String title = "Dataset/Statement "
				+ ds.getItemCount();

			detailChart = ChartFactory.createPieChart(title,ds,true,true,false);
			
			PiePlot pp = (PiePlot) detailChart.getPlot();
			pp.setCircular(true);
			pp.setLabelLinksVisible(false);
			pp.setLabelGenerator(new StandardPieSectionLabelGenerator());
			
			if (addAgree && addDisagree) {
			    pp.setSectionPaint("Agree", Color.green);
			    pp.setSectionPaint("Undecided", Color.yellow);
			    pp.setSectionPaint("Disagree", Color.red);
			} else
			if (addAgree && !addDisagree) {
			    pp.setSectionPaint("Agree", Color.green);
			    pp.setSectionPaint("Undecided", Color.yellow);
			    pp.setSectionPaint("Disagree", Color.orange);
			    pp.setSectionPaint("Strongly Disagree", Color.red);
			} else		
			if (!addAgree && addDisagree) {
			    pp.setSectionPaint("Strongly Agree", Color.green);
			    pp.setSectionPaint("Agree", Color.cyan);
			    pp.setSectionPaint("Undecided", Color.yellow);
			    pp.setSectionPaint("Disagree", Color.orange);
			} 	else {
			    pp.setSectionPaint("Strongly Agree", Color.green);
			    pp.setSectionPaint("Agree", Color.cyan);
			    pp.setSectionPaint("Undecided", Color.yellow);
			    pp.setSectionPaint("Disagree", Color.orange);
			    pp.setSectionPaint("Strongly Disagree", Color.red);
			}
		}

		private void createBar(JFreeChart oldChart) {
			// TODO Auto-generated method stub
			
		}

		private void createStacked(JFreeChart oldChart) {
			CategoryDataset ds = oldChart.getCategoryPlot().getDataset();

			String title = "Dataset/Statement "
					+ ds.getColumnKey(0).toString();
			detailChart.setTitle(title);
			
			CategoryPlot categoryPlot = (CategoryPlot) detailChart.getPlot();
			categoryPlot.getDomainAxis().setTickLabelsVisible(true);
			categoryPlot.getDomainAxis().setVisible(false);
			categoryPlot.getRangeAxis().setVisible(true);
			
			categoryPlot.setDataset(ds);

			StackedBarRenderer render = (StackedBarRenderer) categoryPlot.getRenderer();
			render.setDrawBarOutline(true);
			render.setBaseItemLabelsVisible(true);
			render.setBaseItemLabelGenerator(new StandardCategoryItemLabelGenerator());
		    render.setShadowVisible(true);
		    
		    for (int i = 0; i < 6; i++) {
		    	render.setSeriesPaint(i,oldChart.getCategoryPlot().getRenderer().getSeriesPaint(i));
			}
		    
		    table.setModel(new DetailsTableModel(ds.getColumnKey(0).toString()));

		}
	} 

	static class DetailsTableModel extends AbstractTableModel {

		private static final long serialVersionUID = -2796956447514694452L;
		
		String key;
		
		public DetailsTableModel(String key) {
			this.key = key;
		}

		@Override
		public int getColumnCount() {
			return 2;
		}

		@Override
		public int getRowCount() {
			return 5;
		}

		@Override
		public Object getValueAt(int row, int col) {
			String[] rows = {  "Mode",  "1st Quartile", "Median (2nd Quartile)",  "3rd Quartile",  "Interquartile Range" };		
			
			if (col == 0) {
				return rows[row];
			} else {
				double[] details = detailsTable.get(key);
				return details[row]; 	
			}
		}
	}
}



