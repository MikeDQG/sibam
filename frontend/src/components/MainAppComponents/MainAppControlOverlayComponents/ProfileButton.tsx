import { UserRound } from "lucide-react";
import { Button } from "../../ui/button";

export function ProfileButton({ onClick }: { onClick: () => void }) {
  return (
    <Button
      type='button'
      onClick={onClick}
      className='flex h-10 w-10 items-center justify-center rounded-md bg-red-700 text-white shadow-lg hover:text-red-200'
      aria-label='Profil'>
      <UserRound strokeWidth={1.7} />
    </Button>
  );
}
