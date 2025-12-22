type DividerProps = {
  label?: string;
};

export function Divider({ label }: DividerProps) {
  return (
    <div className="relative">
      <div aria-hidden="true" className="absolute inset-0 flex items-center">
        <div className="w-full border-gray-300 border-t" />
      </div>
      {label ? (
        <div className="relative flex justify-center">
          <span className="bg-white px-2 text-gray-500 text-sm">{label}</span>
        </div>
      ) : null}
    </div>
  );
}
