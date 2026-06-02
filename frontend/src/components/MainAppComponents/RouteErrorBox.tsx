import { AlertTriangle } from "lucide-react";

type RouteErrorBoxProps = {
  code: string;
  message?: string;
};

export const RouteErrorBox = ({ code, message }: RouteErrorBoxProps) => {
  return (
    <div
      role='alert'
      className='mb-5 flex items-start gap-3 rounded-lg border border-red-300 bg-red-50 px-4 py-3 text-red-950 shadow-sm dark:border-red-900/70 dark:bg-red-950/35 dark:text-red-100'>
      <AlertTriangle className='mt-0.5 h-5 w-5 shrink-0 text-red-700 dark:text-red-300' />
      <div className='min-w-0'>
        <p className='text-sm font-semibold'>
          Napaka ob iskanju poti: {code}
          {/*Error while computing path: {code}*/}
        </p>
        {message && (
          <p className='mt-1 text-xs leading-snug text-red-800 dark:text-red-200'>
            {message}
          </p>
        )}
      </div>
    </div>
  );
};
