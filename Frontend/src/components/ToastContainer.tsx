import { useToastStore } from '../stores/toastStore';

export default function ToastContainer() {
  const toasts = useToastStore((state) => state.toasts);
  const removeToast = useToastStore((state) => state.removeToast);

  if (toasts.length === 0) return null;

  return (
    <div className="fixed bottom-4 right-4 z-50 flex flex-col gap-2">
      {toasts.map((toast) => (
        <div
          key={toast.id}
          className="bg-blue-600 text-white px-4 py-3 rounded shadow-lg flex justify-between items-center min-w-[250px]"
        >
          <span className="text-sm font-medium">{toast.text}</span>
          <button
            onClick={() => removeToast(toast.id)}
            className="ml-4 text-white hover:text-gray-200 focus:outline-none"
          >
            &times;
          </button>
        </div>
      ))}
    </div>
  );
}
