// components/Skeleton.jsx
// Reusable skeleton loader component.
// Provides a shimmer effect to indicate loading state.

import './Skeleton.css';

export default function Skeleton({ className = '', width, height, borderRadius, style = {} }) {
  const mergedStyle = {
    width: width || '100%',
    height: height || '20px',
    borderRadius: borderRadius || 'var(--radius-sm)',
    ...style,
  };

  return (
    <div className={`skeleton ${className}`} style={mergedStyle} />
  );
}
