import React from 'react';
import EmptyState from './EmptyState';

function DataTable({ columns, rows, emptyText = 'No records found.' }) {
    if (!rows || rows.length === 0) {
        return <EmptyState text={emptyText} />;
    }

    return (
        <div className="table-wrap">
            <table className="data-table">
                <thead>
                    <tr>
                        {columns.map((column) => (
                            <th key={column.key}>{column.header}</th>
                        ))}
                    </tr>
                </thead>
                <tbody>
                    {rows.map((row, index) => (
                        <tr key={row.id || row.operationId || row.eventId || index}>
                            {columns.map((column) => (
                                <td key={column.key}>
                                    {column.render ? column.render(row) : row[column.key]}
                                </td>
                            ))}
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}

export default DataTable;
