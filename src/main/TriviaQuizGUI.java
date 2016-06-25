package main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

public class TriviaQuizGUI extends JFrame{

	/**
	 *
	 */
	private static final long serialVersionUID = 2560063567392271933L;
	private JPanel contentPane;
	private JTable table;
	private JTextField textField;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args){
		EventQueue.invokeLater(new Runnable(){
			@Override
			public void run(){
				try{
					TriviaQuizGUI frame = new TriviaQuizGUI(Files.readAllLines(Paths.get("input.txt")));
					frame.setVisible(true);
				}
				catch(Exception e){
					e.printStackTrace();
				}
			}
		});
	}

	private static String[] generateColumns(int count){
		String[] cols = new String[count];
		for(int i = 0; i < count; i++){
			cols[i] = String.valueOf(i);
		}
		return cols;
	}

	//private List<String> data;
	private Object[][] tableData;
	private String[] columnNames = generateColumns(14);
	private AbstractTableModel model;
	private JPanel panel;
	private long startTime = 0;
	private JLabel label;

	public TriviaQuizGUI(){
		this(Arrays.asList("Abacus", "Banana", "Cello", "Door", "Elephant"));
	}

	/**
	 * Create the frame.
	 */
	public TriviaQuizGUI(List<String> stringData){
		List<Answer> data = stringData.stream().map(string -> new Answer(string)).collect(Collectors.toList());

		//this.data = data;

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(25, 25, 1360, 920);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);

		model = new AbstractTableModel(){
			private static final long serialVersionUID = -157414300908240564L;

			@Override
			public String getColumnName(int col){
				return columnNames[col].toString();
			}

			@Override
			public int getRowCount(){
				return tableData.length;
			}

			@Override
			public int getColumnCount(){
				return columnNames.length;
			}

			@Override
			public Object getValueAt(int row, int col){
				return tableData[row][col];
			}

			@Override
			public boolean isCellEditable(int row, int col){
				return true;
			}

			@Override
			public void setValueAt(Object value, int row, int col){
				tableData[row][col] = value;
				fireTableCellUpdated(row, col);
				save();
			}
		};

		int cols = columnNames.length;
		//int rows = getRows(data.size(), cols);
		tableData = load();
		startTime = System.currentTimeMillis() - startTime;

		if(tableData == null){
			tableData = emptyDataFromSize(data.size(), cols);
		}
		//data = dataFromStrings(stringData, columnNames.length);
		table = new JTable(model);
		table.setBorder(new LineBorder(Color.BLACK, 1));
		table.setGridColor(Color.black);
		table.setCellSelectionEnabled(true);
		table.setRowSelectionAllowed(false);
		table.setColumnSelectionAllowed(false);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer(){
			/**
			 *
			 */
			private static final long serialVersionUID = 6347701731742929492L;

			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
					boolean hasFocus, int row, int column){
				Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

				if(hasFocus && table.getSelectedRow() == row && table.getSelectedColumn() == column){
					c.setBackground(table.getSelectionBackground());
				}
				else if(tableData[row][column].equals("N/A")){
					c.setBackground(Color.LIGHT_GRAY);
				}
				else if(!tableData[row][column].equals("")){
					c.setBackground(Color.YELLOW);
				}
				else{
					c.setBackground(table.getBackground());
				}

				return c;
			}
		});

		JScrollPane scrollPane = new JScrollPane(table);

		//scrollPane.add(table);
		contentPane.add(scrollPane, BorderLayout.CENTER);

		panel = new JPanel();
		panel.setLayout(new BorderLayout(0, 0));
		contentPane.add(panel, BorderLayout.NORTH);

		textField = new JTextField();
		panel.add(textField, BorderLayout.CENTER);
		textField.setColumns(10);

		label = new JLabel("0:00:00");
		panel.add(label, BorderLayout.WEST);
		textField.getDocument().addDocumentListener(new DocumentListener(){

			@Override
			public void insertUpdate(DocumentEvent e){
				changedUpdate(e);
			}

			@Override
			public void removeUpdate(DocumentEvent e){
				changedUpdate(e);
			}

			@Override
			public void changedUpdate(DocumentEvent e){
				int index = data.indexOf(new Answer(textField.getText()));
				if(index >= 0){
					int cols = columnNames.length;
					Coordinate transformed = tableIndex(index, data.size(), cols);
					model.setValueAt(data.get(index).getText(), transformed.row, transformed.col);
					EventQueue.invokeLater(() -> textField.setText(""));
				}
			}
		});
		SwingWorker<String, String> worker = new SwingWorker<String, String>(){

			@Override
			protected void process(List<String> list){
				label.setText(list.get(list.size() - 1));
			}

			@Override
			protected String doInBackground() throws Exception{
				while(true){
					Thread.sleep(10);
					publish(calculateTime(System.currentTimeMillis() - startTime));
				}
			}
		};
		worker.execute();
	}

	public Object[][] load(){
		try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream("save.dat"))){
			Object[][] o = (Object[][]) ois.readObject();
			startTime = ois.readLong();
			return o;
		}
		catch(ClassCastException e){
			e.printStackTrace();
		}
		catch(FileNotFoundException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch(IOException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch(ClassNotFoundException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public void save(){
		try(ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("save.dat"))){
			oos.writeObject(tableData);
			oos.writeLong(System.currentTimeMillis() - startTime);
		}
		catch(FileNotFoundException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch(IOException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static String calculateTime(long l){
		long hr = TimeUnit.MILLISECONDS.toHours(l);
		long min = TimeUnit.MILLISECONDS.toMinutes(l - TimeUnit.HOURS.toMillis(hr));
		long sec = TimeUnit.MILLISECONDS.toSeconds(l - TimeUnit.HOURS.toMillis(hr) - TimeUnit.MINUTES.toMillis(min));
		long ms = TimeUnit.MILLISECONDS.toMillis(
				l - TimeUnit.HOURS.toMillis(hr) - TimeUnit.MINUTES.toMillis(min) - TimeUnit.SECONDS.toMillis(sec));
		return String.format("%d:%02d:%02d.%03d", hr, min, sec, ms);
	}

	public static Coordinate tableIndex(int index, int length, int columns){
		if(index == 0){
			return new Coordinate(0, 0);
		}
		int rows = getRows(length, columns);
		int stringIndex = 0;
		int i = 0;
		for(; i < rows * columns; i++){
			if(stringIndex >= length || i % rows == rows - 1 && i / rows > (i - 1) % rows){
				//
			}
			else{
				stringIndex++;
				if(stringIndex == index){
					break;
				}
			}
		}
		return new Coordinate(i % rows, i / rows);
	}

	public static int getRows(int length, int columns){
		return (length + columns - 1) / columns;
	}

	public static Object[][] emptyData(int rows, int columns){
		Object[][] objs = new Object[rows][columns];
		for(int i = 0; i < rows * columns; i++){
			objs[i % rows][i / rows] = "";
		}
		return objs;
	}

	public static Object[][] emptyDataFromSize(int length, int columns){
		int rows = getRows(length, columns);
		Object[][] objs = new Object[rows][columns];
		int stringIndex = 0;
		for(int i = 0; i < rows * columns; i++){
			String out;
			if(stringIndex >= length || i % rows == rows - 1 && i / rows > (i - 1) % rows){
				out = "N/A";
			}
			else{
				out = "";
				stringIndex++;
			}
			objs[i % rows][i / rows] = out;
		}
		return objs;
	}

	private static class Coordinate{
		public int row;
		public int col;

		public Coordinate(int row, int col){
			this.row = row;
			this.col = col;
		}
	}

	public static class Answer{
		private String text;

		public Answer(String text){
			this.text = text;
		}

		public String getText(){
			return text;
		}

		@Override
		public String toString(){
			return getText();
		}

		@Override
		public boolean equals(Object o){
			if(o instanceof Answer){
				return getText().equalsIgnoreCase(((Answer) o).getText());
			}
			return false;
		}

		@Override
		public int hashCode(){
			return getText().toLowerCase().hashCode();
		}
	}

}
