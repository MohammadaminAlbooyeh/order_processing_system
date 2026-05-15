import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { listOrders } from '../api/orders'
import type { Order } from '../types'

const STATUS_COLORS: Record<string, string> = {
  pending: 'badge-warning',
  processing: 'badge-info',
  shipped: 'badge-primary',
  delivered: 'badge-success',
  cancelled: 'badge-error',
}

export default function DashboardPage() {
  const navigate = useNavigate()
  const [orders, setOrders] = useState<Order[]>([])
  const [total, setTotal] = useState(0)
  const [statusFilter, setStatusFilter] = useState('')
  const [loading, setLoading] = useState(true)

  function fetchOrders() {
    setLoading(true)
    listOrders({ status: statusFilter || undefined, limit: 50 })
      .then((res) => {
        setOrders(res.orders)
        setTotal(res.total)
      })
      .catch(() => {})
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    fetchOrders()
  }, [statusFilter])

  return (
    <div>
      <div className="flex-between">
        <h1>Orders</h1>
        <button className="btn" onClick={() => navigate('/orders/new')}>
          + New Order
        </button>
      </div>

      <div className="filter-bar">
        <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
          <option value="">All Statuses</option>
          <option value="pending">Pending</option>
          <option value="processing">Processing</option>
          <option value="shipped">Shipped</option>
          <option value="delivered">Delivered</option>
          <option value="cancelled">Cancelled</option>
        </select>
      </div>

      {loading ? (
        <p className="text-center">Loading orders...</p>
      ) : orders.length === 0 ? (
        <div className="card text-center">
          <p>No orders found.</p>
          <button className="btn" onClick={() => navigate('/orders/new')}>
            Create your first order
          </button>
        </div>
      ) : (
        <div className="table-wrapper">
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>Items</th>
                <th>Destination</th>
                <th>Weight</th>
                <th>Status</th>
                <th>Created</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {orders.map((order) => (
                <tr key={order.id}>
                  <td>#{order.id}</td>
                  <td>{order.items?.length ?? 0} item(s)</td>
                  <td className="text-truncate">{order.destination}</td>
                  <td>{order.weight} kg</td>
                  <td>
                    <span className={`badge ${STATUS_COLORS[order.status] || 'badge-default'}`}>
                      {order.status}
                    </span>
                  </td>
                  <td>{order.created_at ? new Date(order.created_at).toLocaleDateString() : '-'}</td>
                  <td>
                    <button className="btn btn-sm" onClick={() => navigate(`/orders/${order.id}`)}>
                      View
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <p className="text-muted">{total} total order(s)</p>
        </div>
      )}
    </div>
  )
}
