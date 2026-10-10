import CatalogPage from './CatalogPage.jsx'
import ProductPage from './ProductPage.jsx'
import ComparePage from './ComparePage.jsx'
import InventoryPage from './admin/InventoryPage.jsx'

export default [
  { path: '/', element: <CatalogPage /> },
  { path: '/products/:sku', element: <ProductPage /> },
  { path: '/compare', element: <ComparePage /> },
  { path: '/admin/inventory', element: <InventoryPage />, staff: true },
]

export const nav = [
  { label: 'Shop', to: '/' },
  { label: 'Inventory', to: '/admin/inventory', staff: true },
]
