export const formatCurrency = (value) => {
  if (value == null || Number.isNaN(Number(value))) return "공고문 확인";
  return `${Math.round(Number(value) / 10000).toLocaleString("ko-KR")}만원`;
};

export const formatRent = (value) => {
  if (value == null || Number.isNaN(Number(value))) return "공고문 확인";
  return `${Number(value).toLocaleString("ko-KR")}원`;
};

export const createIcon = (pathMarkup, attrs = "") => `<svg viewBox="0 0 24 24" ${attrs}>${pathMarkup}</svg>`;
export const heartIcon = () =>
  createIcon(
    '<path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>',
    'fill="none" stroke="currentColor" stroke-width="2"'
  );

export const queryParam = (name, fallback = "") => {
  const params = new URLSearchParams(window.location.search);
  return params.get(name) ?? fallback;
};

export const setQueryParams = (patch) => {
  const url = new URL(window.location.href);
  Object.entries(patch).forEach(([key, value]) => {
    if (value === "" || value == null || value === "all") {
      url.searchParams.delete(key);
      return;
    }
    url.searchParams.set(key, value);
  });
  window.location.href = url.toString();
};
