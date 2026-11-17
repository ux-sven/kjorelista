// Stilark kopiert uendret fra kjoreliste-app/src/lib/report.ts (som igjen er fra webløsningen).
enum ReportStyle {
    static let css = #"""
        body {
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Arial, sans-serif;
            margin: 0;
            padding: 20px;
            line-height: 1.4;
            font-size: 13px;
            color: #1a1a1a;
        }
        
        .page-header {
            text-align: center;
            margin-bottom: 40px;
            border-bottom: 3px solid #2563eb;
            padding-bottom: 25px;
        }
        
        .page-header h1 {
            margin: 0 0 12px 0;
            color: #1e293b;
            font-size: 28px;
            font-weight: 600;
        }
        
        .page-header h2 {
            margin: 0 0 8px 0;
            color: #475569;
            font-size: 20px;
            font-weight: 500;
        }
        
        .page-header .user-info {
            color: #64748b;
            font-size: 14px;
            margin-top: 8px;
        }

        /* Table styles */
        .trips-table {
            width: 100%;
            border-collapse: collapse;
            margin-bottom: 30px;
            background: white;
            border-radius: 8px;
            overflow: hidden;
            box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
        }
        
        .trips-table th {
            background: linear-gradient(135deg, #f1f5f9 0%, #e2e8f0 100%);
            color: #334155;
            font-weight: 600;
            padding: 12px 8px;
            text-align: left;
            border-bottom: 2px solid #cbd5e1;
            font-size: 11px;
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }
        
        .trips-table td {
            padding: 10px 8px;
            border-bottom: 1px solid #f1f5f9;
            vertical-align: top;
            font-size: 12px;
        }
        
        .trips-table tbody tr {
            transition: background-color 0.2s ease;
        }
        
        .trips-table tbody tr:nth-child(even) {
            background-color: #f8fafc;
        }
        
        .trips-table tbody tr:hover {
            background-color: #f1f5f9;
        }
        
        /* Summary row styles */
        .summary-row {
            background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%) !important;
            border-top: 2px solid #cbd5e1 !important;
            font-weight: 600 !important;
        }
        
        .summary-row td {
            padding: 12px 8px !important;
            border-bottom: 2px solid #cbd5e1 !important;
            font-size: 12px !important;
            color: #374151 !important;
        }
        
        .summary-label {
            font-weight: 600 !important;
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }
        
        /* Specific column styles */
        .date-cell {
            font-weight: 500;
            color: #475569;
            white-space: nowrap;
            min-width: 85px;
        }
        
        .route-cell {
            max-width: 160px;
            word-wrap: break-word;
            font-weight: 500;
            color: #1e293b;
        }
        
        .number-cell {
            text-align: right;
            font-family: 'SF Mono', 'Monaco', 'Cascadia Code', monospace;
            font-weight: 500;
            color: #374151;
        }
        
        .expense-cell {
            font-size: 11px;
            max-width: 100px;
            word-wrap: break-word;
            color: #6b7280;
        }
        
        /* Comment row styles */
        .comment-row {
            background-color: #f8fafc !important;
            border-top: none !important;
        }
        
        .comment-row td {
            padding: 8px 12px 12px 12px;
            border-bottom: 2px solid #e2e8f0;
            font-style: italic;
            color: #64748b;
            font-size: 11px;
            line-height: 1.4;
        }
        
        .comment-label {
            font-weight: 600;
            color: #475569;
            margin-right: 6px;
        }

        /* Page break for summary */
        .summary-page {
            page-break-before: always;
            margin-top: 40px;
        }
        
        .summary {
            background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
            padding: 30px;
            border-radius: 12px;
            border: 2px solid #e2e8f0;
            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);
        }
        
        .summary h3 {
            margin: 0 0 25px 0;
            color: #1e293b;
            font-size: 22px;
            font-weight: 600;
            text-align: center;
            border-bottom: 2px solid #2563eb;
            padding-bottom: 12px;
        }
        
        .summary-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 20px;
        }
        
        .summary-item {
            background: white;
            padding: 20px;
            border-radius: 8px;
            text-align: center;
            border: 1px solid #e2e8f0;
            box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
        }
        
        .summary-value {
            font-size: 24px;
            font-weight: 700;
            color: #2563eb;
            margin-bottom: 8px;
            font-family: 'SF Mono', 'Monaco', 'Cascadia Code', monospace;
        }
        
        .summary-label {
            font-size: 13px;
            color: #64748b;
            font-weight: 500;
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }

        .footer {
            margin-top: 50px;
            text-align: center;
            font-size: 11px;
            color: #94a3b8;
            border-top: 1px solid #e2e8f0;
            padding-top: 20px;
        }
        
        .no-trips {
            text-align: center;
            padding: 60px 20px;
            color: #64748b;
            font-size: 16px;
            background: #f8fafc;
            border-radius: 8px;
            border: 2px dashed #cbd5e1;
        }

        /* Print optimizations */
        @media print {
            body { 
                margin: 0; 
                padding: 15px;
                font-size: 11px;
                -webkit-print-color-adjust: exact;
                color-adjust: exact;
            }
            
            .page-header h1 { font-size: 24px; }
            .page-header h2 { font-size: 18px; }
            
            .trips-table th { 
                font-size: 9px; 
                padding: 8px 6px;
            }
            
            .trips-table td { 
                font-size: 10px; 
                padding: 6px 4px;
            }
            
            .summary-row td {
                font-size: 10px !important;
                padding: 10px 6px !important;
            }
            
            .comment-row td {
                font-size: 9px;
                padding: 6px 8px 8px 8px;
            }
            
            .summary {
                padding: 20px;
                margin-top: 30px;
            }
            
            .summary h3 { font-size: 18px; }
            .summary-value { font-size: 20px; }
            .summary-label { font-size: 11px; }
            
            .no-print { display: none; }
        }
        
        /* Mobile and iOS Safari specific optimizations */
        @media screen and (max-width: 768px) {
            body {
                font-size: 12px;
                line-height: 1.3;
            }
            
            .page-header h1 { font-size: 22px; }
            .page-header h2 { font-size: 16px; }
            
            .trips-table {
                font-size: 11px;
            }
            
            .trips-table th {
                padding: 8px 4px;
                font-size: 10px;
            }
            
            .trips-table td {
                padding: 6px 4px;
                font-size: 11px;
            }
            
            .summary {
                padding: 16px;
            }
            
            .summary-grid {
                grid-template-columns: repeat(2, 1fr);
                gap: 12px;
            }
            
            .summary-item {
                padding: 12px;
            }
            
            .summary-value {
                font-size: 18px;
            }
        }
        
        @page {
            margin: 1.5cm;
            size: A4;
        }
"""#
}
