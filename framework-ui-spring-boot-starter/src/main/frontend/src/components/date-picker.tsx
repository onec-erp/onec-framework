import { useState } from "react";
import { format, isValid } from "date-fns";
import { CalendarIcon } from "lucide-react";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Calendar } from "@/components/ui/calendar";
import { Input } from "@/components/ui/input";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";

interface DatePickerProps {
  value?: string;
  onChange: (val: string) => void;
  includeTime?: boolean;
}

export function DatePicker({ value, onChange, includeTime = false }: DatePickerProps) {
  const [open, setOpen] = useState(false);

  const date = value ? new Date(value) : undefined;
  const hours = date && isValid(date) ? String(date.getHours()).padStart(2, "0") : "00";
  const minutes = date && isValid(date) ? String(date.getMinutes()).padStart(2, "0") : "00";

  const handleDateSelect = (selected: Date | undefined) => {
    if (!selected) return;
    if (includeTime && date && isValid(date)) {
      selected.setHours(date.getHours(), date.getMinutes());
    }
    emitValue(selected);
    if (!includeTime) setOpen(false);
  };

  const handleTimeChange = (h: string, m: string) => {
    const d = date && isValid(date) ? new Date(date) : new Date();
    d.setHours(parseInt(h) || 0, parseInt(m) || 0, 0, 0);
    emitValue(d);
  };

  const emitValue = (d: Date) => {
    if (includeTime) {
      onChange(format(d, "yyyy-MM-dd'T'HH:mm"));
    } else {
      onChange(format(d, "yyyy-MM-dd"));
    }
  };

  const displayText = date && isValid(date)
    ? includeTime
      ? format(date, "PPP HH:mm")
      : format(date, "PPP")
    : "Pick a date";

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button
          variant="outline"
          className={cn(
            "w-full justify-start text-left font-normal",
            !value && "text-muted-foreground"
          )}
        >
          <CalendarIcon className="mr-2 h-4 w-4" />
          {displayText}
        </Button>
      </PopoverTrigger>
      <PopoverContent className="w-auto p-0">
        <Calendar
          mode="single"
          selected={date}
          onSelect={handleDateSelect}
          initialFocus
        />
        {includeTime && (
          <div className="flex items-center gap-2 border-t px-4 py-3">
            <Input
              type="number"
              min={0}
              max={23}
              className="w-16"
              value={hours}
              onChange={(e) => handleTimeChange(e.target.value, minutes)}
            />
            <span className="text-sm font-medium">:</span>
            <Input
              type="number"
              min={0}
              max={59}
              className="w-16"
              value={minutes}
              onChange={(e) => handleTimeChange(hours, e.target.value)}
            />
          </div>
        )}
      </PopoverContent>
    </Popover>
  );
}
