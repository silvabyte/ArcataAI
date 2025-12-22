import { t } from "@arcata/translate";

export type MatchScoreIndicatorProps = {
  score: number;
  postedDaysAgo: number;
};

export function MatchScoreIndicator({
  score,
  postedDaysAgo,
}: MatchScoreIndicatorProps) {
  const getPostedText = () => {
    if (postedDaysAgo === 0) {
      return t("pages.jobStream.card.postedToday");
    }
    if (postedDaysAgo >= 7) {
      const weeks = Math.floor(postedDaysAgo / 7);
      return t("pages.jobStream.card.postedWeeksAgo", { weeks });
    }
    return t("pages.jobStream.card.postedDaysAgo", { days: postedDaysAgo });
  };

  return (
    <div className="text-right">
      <div className="flex items-baseline justify-end gap-1">
        <span className="font-bold text-2xl text-gray-900">{score}%</span>
        <span className="text-gray-500 text-xs">
          {t("pages.jobStream.card.match")}
        </span>
      </div>
      <p className="text-gray-500 text-xs">{getPostedText()}</p>
    </div>
  );
}
