// components/TransactionChart.jsx
// A beautiful area chart showing deposits (credits) vs withdrawals (debits) over time.

import { useMemo } from 'react';
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer
} from 'recharts';

export default function TransactionChart({ transactions }) {
  // Aggregate transactions by date
  const chartData = useMemo(() => {
    if (!transactions || transactions.length === 0) return [];

    // Group by Date (YYYY-MM-DD)
    const grouped = {};
    
    // Sort transactions oldest to newest for the chart
    const sortedTxs = [...transactions].sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));

    sortedTxs.forEach(tx => {
      const dateStr = new Date(tx.createdAt).toLocaleDateString('en-IN', {
        month: 'short', day: 'numeric'
      });
      
      if (!grouped[dateStr]) {
        grouped[dateStr] = { date: dateStr, credits: 0, debits: 0 };
      }
      
      const amount = Number(tx.amount);
      if (tx.type === 'CREDIT') {
        grouped[dateStr].credits += amount;
      } else {
        grouped[dateStr].debits += amount;
      }
    });

    return Object.values(grouped);
  }, [transactions]);

  if (chartData.length === 0) {
    return (
      <div className="empty-state" style={{ height: 250, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        Not enough data to display chart.
      </div>
    );
  }

  // Custom tooltip to match dark theme
  const CustomTooltip = ({ active, payload, label }) => {
    if (active && payload && payload.length) {
      return (
        <div style={{
          background: 'rgba(15, 23, 42, 0.9)',
          border: '1px solid rgba(255,255,255,0.1)',
          borderRadius: '8px',
          padding: '12px',
          boxShadow: '0 8px 32px rgba(0,0,0,0.5)',
          backdropFilter: 'blur(8px)'
        }}>
          <p style={{ margin: '0 0 8px 0', color: '#94a3b8', fontSize: '0.85rem' }}>{label}</p>
          {payload.map((entry, index) => (
            <p key={index} style={{ margin: '4px 0', color: entry.color, fontSize: '0.9rem', fontWeight: 600 }}>
              {entry.name}: ₹{entry.value.toLocaleString('en-IN')}
            </p>
          ))}
        </div>
      );
    }
    return null;
  };

  return (
    <div style={{ width: '100%', height: 280, marginTop: '20px' }}>
      <ResponsiveContainer width="100%" height="100%">
        <AreaChart data={chartData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
          <defs>
            <linearGradient id="colorCredit" x1="0" y1="0" x2="0" y2="1">
              <stop offset="5%" stopColor="#34d399" stopOpacity={0.3}/>
              <stop offset="95%" stopColor="#34d399" stopOpacity={0}/>
            </linearGradient>
            <linearGradient id="colorDebit" x1="0" y1="0" x2="0" y2="1">
              <stop offset="5%" stopColor="#fca5a5" stopOpacity={0.3}/>
              <stop offset="95%" stopColor="#fca5a5" stopOpacity={0}/>
            </linearGradient>
          </defs>
          <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" vertical={false} />
          <XAxis 
            dataKey="date" 
            stroke="#64748b" 
            fontSize={12} 
            tickLine={false} 
            axisLine={false} 
            dy={10} 
          />
          <YAxis 
            stroke="#64748b" 
            fontSize={12} 
            tickLine={false} 
            axisLine={false} 
            tickFormatter={(value) => `₹${value >= 1000 ? (value/1000) + 'k' : value}`}
          />
          <Tooltip content={<CustomTooltip />} />
          
          <Area 
            type="monotone" 
            dataKey="credits" 
            name="Credits" 
            stroke="#34d399" 
            strokeWidth={2}
            fillOpacity={1} 
            fill="url(#colorCredit)" 
          />
          <Area 
            type="monotone" 
            dataKey="debits" 
            name="Debits" 
            stroke="#fca5a5" 
            strokeWidth={2}
            fillOpacity={1} 
            fill="url(#colorDebit)" 
          />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  );
}
