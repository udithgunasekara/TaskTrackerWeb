import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../stores/authStore';

export default function Navbar() {
  const { user, logout } = useAuthStore();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  if (!user) return null;

  return (
    <nav className="bg-gray-800 text-white p-4 flex justify-between items-center">
      <div className="font-bold text-xl">Task Tracker</div>
      <div className="flex items-center gap-4">
        <span>{user.fullName}</span>
        <span className="bg-blue-600 px-2 py-1 rounded text-xs">{user.role}</span>
        <button onClick={handleLogout} className="bg-red-600 hover:bg-red-700 px-3 py-1 rounded">
          Logout
        </button>
      </div>
    </nav>
  );
}
