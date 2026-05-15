import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { getOrder, updateOrder, cancelOrder } from '../api/orders'
import type { Order } from '../types'

const STATUS_COLORS: Record<string, string> = {
  pending: 'badge-warning',
  processing: 'badge-info',
  shipped: 'badge-primary',
  delivered: 'badge-success',
  cancelled: 'badge-error',
}

const PRODUCT_NAMES: Record<number, string> = {
  10: 'Wireless Mouse',
  11: 'Mechanical Keyboard',
  12: 'USB-C Hub',
  13: 'Webcam HD',
  14: 'Noise-Canceling Headphones',
}

export default function OrderDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [order, setOrder] = useState<Order | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!id) return
    getOrder(parseInt(id))
      .then(setOrder)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false))
  }, [id])

  async function handleCancel() {
    if (!confirm('Cancel this order?')) return
    try {
      await cancelOrder(order!.id)
      setOrder({ ...order!, status: 'cancelled' })
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to cancel')
    }
  }

  async function handleAdvance() {
    const transitions: Record<string, string> = {
      pending: 'processing',
      processing: 'shipped',
      shipped: 'delivered',
    }
    const nextStatus = transitions[order!.status]
    if (!nextStatus) return
    try {
      const updated = await updateOrder(order!.id, { status: nextStatus })
      setOrder(updated)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update')
    }
  }

  if (loading) return <p className="text-center">Loading order...</p>
  if (error) return <div className="alert alert-error">{error}</div>
  if (!order) return <p className="text-center">Order not found.</p>

  const nextTransition: Record<string, string> = {
    pending: 'processing',
    processing: 'shipped',
    shipped: 'delivered',
  }

  return (
    <div>
      <button className="btn btn-sm" onClick={() => navigate('/dashboard')}>
        &larr; Back
      </button>

      <div className="order-header">
        <h1>Order #{order.id}</h1>
        <span className={`badge badge-lg ${STATUS_COLORS[order.status] || 'badge-default'}`}>
          {order.status}
        </span>
      </div>

      <div className="card-grid">
        <div className="card">
          <h3>Details</h3>
          <dl>
            <dt>User ID</dt>
            <dd>{order.user_id}</dd>
            <dt>Destination</dt>
            <dd>{order.destination || '-'}</dd>
            <dt>Weight</dt>
            <dd>{order.weight} kg</dd>
            <dt>Created</dt>
            <dd>{order.created_at ? new Date(order.created_at).toLocaleString() : '-'}</dd>
            <dt>Updated</dt>
            <dd>{order.updated_at ? new Date(order.updated_at).toLocaleString() : '-'}</dd>
          </dl>
        </div>

        <div className="card">
          <h3>Items</h3>
          {order.items && order.items.length > 0 ? (
            <table>
              <thead>
                <tr>
                  <th>Product</th>
                  <th>Qty</th>
                </tr>
              </thead>
              <tbody>
                {order.items.map((item: any, i: number) => (
                  <tr key={i}>
                    <td>{PRODUCT_NAMES[item.product_id] || `Product #${item.product_id}`}</td>
                    <td>{item.quantity}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : (
            <p className="text-muted">No items</p>
          )}
        </div>
      </div>

      <div className="card">
        <h3>Actions</h3>
        <div className="actions">
          {nextTransition[order.status] && (
            <button className="btn" onClick={handleAdvance}>
              Mark as {nextTransition[order.status]}
            </button>
          )}
          {(order.status === 'pending' || order.status === 'processing') && (
            <button className="btn btn-danger" onClick={handleCancel}>
              Cancel Order
            </button>
          )}
        </div>
      </div>
    </div>
  )
}
