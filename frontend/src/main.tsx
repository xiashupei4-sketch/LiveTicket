import React from 'react'
import ReactDOM from 'react-dom/client'
import { ConfigProvider } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import App from './App'
import { useAuthStore } from './store/authStore'

useAuthStore.getState().hydrate()

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ConfigProvider
      locale={zhCN}
      theme={{
        token: {
          colorPrimary: '#635BFF',
          colorInfo: '#635BFF',
          colorSuccess: '#16A36A',
          colorWarning: '#E99A14',
          colorError: '#E5484D',
          borderRadius: 8,
          colorBgLayout: '#F7F8FA',
          colorText: '#111318',
          colorTextSecondary: '#60646C',
          colorTextTertiary: '#9297A1',
          colorBorder: '#E8EAF0',
          colorBorderSecondary: '#E8EAF0',
          fontFamily: "Inter, 'PingFang SC', 'Microsoft YaHei', system-ui, sans-serif"
        }
      }}
    >
      <App />
    </ConfigProvider>
  </React.StrictMode>
)
