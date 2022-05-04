package blenderparallelrendering;

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

    public CustomJTable(Object[][] data, String[] columns) {
        super(data, columns);
        defModel = new DefaultTableModel(columns, 0);
        super.dataModel = defModel;
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

}
