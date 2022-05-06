package blenderparallelrendering;

import java.util.ArrayList;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

/**
 * Simple extension of the JTable, with a non-editable last column.
 *
 * @author arthu
 */
public class CustomJTable extends JTable {

    DefaultTableModel defModel;

    private ArrayList<Subscriber> listeners;

    public CustomJTable(Object[][] data, String[] columns) {
        super(data, columns);
        defModel = new DefaultTableModel(columns, 0);
        super.dataModel = defModel;
        listeners = new ArrayList<>();
    }

    @Override
    public TableModel getModel() {
        return super.dataModel;
    }

    /**
     * A cell is editable, except when it is in the Action or Details column.
     *
     * @param row
     * @param col
     * @return true when the cell is editable.
     */
    @Override
    public boolean isCellEditable(int row, int col) {
        String header = getHeader(col);
        return (!header.equals("Action") && !header.equals("Details") && !header.equals("Done/Total"));
    }

    /**
     * Return the header (title) of a given column.
     *
     * @param col
     * @return the header of the column.
     */
    public String getHeader(int col) {
        return (String) this.columnModel.getColumn(col).getHeaderValue();
    }

    public void setColumnWidth() {
        columnModel.getColumn(0).setPreferredWidth(300);
    }

    @Override
    public void setValueAt(Object newValue, int rowIndex, int colIndex) {
        super.setValueAt(newValue, rowIndex, colIndex);

        String notification = "";

        switch (colIndex) {
        case 0:
            notification = "FILENAME_CHANGED " + rowIndex + " " + getValueAt(rowIndex, colIndex);
            break;
        case 2:
            // Changed first frame
            notification = "SET_FIRST_FRAME " + rowIndex + " " + getValueAt(rowIndex, colIndex);
            break;
        case 3:
            // Changed last frame
            notification = "SET_LAST_FRAME " + rowIndex + " " + getValueAt(rowIndex, colIndex);
            break;
        default:
            break;
        }
        notifyListeners(notification);
    }

    // Add a new listener
    public void addListener(Subscriber s) {
        if (!listeners.contains(s)) {
            listeners.add(s);
        }
    }

    private void notifyListeners(String string) {
        for (Subscriber s : listeners) {
            s.update(string);
        }
    }

}
