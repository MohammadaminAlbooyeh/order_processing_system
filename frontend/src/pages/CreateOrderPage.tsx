import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { createOrder } from '../api/orders'
import type { OrderItem } from '../types'

const PRODUCTS = [
  { id: 10, name: 'Wireless Mouse' },
  { id: 11, name: 'Mechanical Keyboard' },
  { id: 12, name: 'USB-C Hub' },
  { id: 13, name: 'Webcam HD' },
  { id: 14, name: 'Noise-Canceling Headphones' },
]

export default function CreateOrderPage() {
  const navigate = useNavigate()

  const [destination, setDestination] = useState('')
  const [weight, setWeight] = useState('1.0')
  const [items, setItems] = useState<OrderItem[]>([{ product_id: 10, quantity: 1 }])
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  function addItem() {
    setItems([...items, { product_id: 10, quantity: 1 }])
  }

  function removeItem(index: number) {
    setItems(items.filter((_, i) => i !== index))
  }

  function updateItem(index: number, field: keyof OrderItem, value: number) {
    const updated = [...items]
    updated[index] = { ...updated[index], [field]: value }
    setItems(updated)
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const order = await createOrder({
        destination,
        weight: parseFloat(weight),
        items,
      })
      navigate(`/orders/${order.id}`)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create order')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div>
      <h1>New Order</h1>
      <form className="card" onSubmit={handleSubmit}>
        {error && <div className="alert alert-error">{error}</div>}

        <label>
          Destination
          <input
            type="text"
            value={destination}
            onChange={(e) => setDestination(e.target.value)}
            required
            minLength={5}
            placeholder="123 Main St, City, Country"
          />
        </label>

        <label>
          Weight (kg)
          <input
            type="number"
            value={weight}
            onChange={(e) => setWeight(e.target.value)}
            required
            min="0"
            step="0.1"
          />
        </label>

        <div className="section-header">
          <h3>Items</h3>
          <button type="button" className="btn btn-sm" onClick={addItem}>
            + Add Item
          </button>
        </div>

        {items.map((item, i) => (
          <div key={i} className="item-row">
            <select
              value={item.product_id}
              onChange={(e) => updateItem(i, 'product_id', parseInt(e.target.value))}
            >
              {PRODUCTS.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.name}
                </option>
              ))}
            </select>
            <input
              type="number"
              value={item.quantity}
              onChange={(e) => updateItem(i, 'quantity', parseInt(e.target.value) || 1)}
              min={1}
              className="qty-input"
            />
            <button type="button" className="btn btn-sm btn-danger" onClick={() => removeItem(i)}>
              Remove
            </button>
          </div>
        ))}

        <button className="btn" type="submit" disabled={loading}>
          {loading ? 'Creating...' : 'Create Order'}
        </button>
      </form>
    </div>
  )
}
