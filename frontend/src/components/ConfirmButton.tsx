import { useState, type ComponentProps } from 'react'
import { Button } from './ui/button'

/** Two-step button for destructive actions: the first click arms it, the second confirms. */
export function ConfirmButton({ children, onClick, ...props }: ComponentProps<typeof Button>) {
  const [armed, setArmed] = useState(false)
  return (
    <Button
      {...props}
      onBlur={() => setArmed(false)}
      onClick={(e) => {
        setArmed(!armed)
        if (armed) onClick?.(e)
      }}
    >
      {armed ? 'Click again to confirm' : children}
    </Button>
  )
}
