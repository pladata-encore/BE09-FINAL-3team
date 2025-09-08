"use client";

import styles from "../styles/SearchAndSort.module.css"

const SORT_OPTIONS = [
  { value: "followers", label: "팔로워 순" },
  { value: "costHigh", label: "단가 높은 순"},
  { value: "costLow", label: "단가 낮은 순"}
];

export default function SearchAndSort({ searchQuery, setSearchQuery, sortBy, setSortBy }) {

  return(
    <div className={styles.searchAndSort}>
      <div className={styles.searchContainer}>
        <div className={styles.searchBox}>
          <div className={styles.searchIcon}>
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
              <path
                d="M7.4 0C11.4794 0 14.8 3.32061 14.8 7.4C14.8 9.16532 14.1794 10.7836 13.1446 12.0279L15.9081 14.7914C16.2327 15.116 16.2327 15.6433 15.9081 15.9679C15.5834 16.2926 15.0561 16.2926 14.7315 15.9679L11.968 13.2044C10.7237 14.2392 9.10538 14.8 7.4 14.8C3.32061 14.8 0 11.4794 0 7.4C0 3.32061 3.32061 0 7.4 0ZM7.4 1.48C4.13401 1.48 1.48 4.13401 1.48 7.4C1.48 10.666 4.13401 13.32 7.4 13.32C10.666 13.32 13.32 10.666 13.32 7.4C13.32 4.13401 10.666 1.48 7.4 1.48Z"
                fill="#9CA3AF"
              />
            </svg>
          </div>
          <input
            type="text"
            placeholder="펫스타 이름을 검색해보세요"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className={styles.searchInput}
          />
        </div>
      </div>

      <div className={styles.sortContainer}>
        <select
          value={sortBy}
          onChange={(e) => setSortBy(e.target.value)}
          className={styles.sortSelect}
        >
           {SORT_OPTIONS.map(option => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
        <div className={styles.selectIcon}>
          <svg width="16" height="10" viewBox="0 0 16 10" fill="none">
            <path
              d="M1.5 1.25L8 7.75L14.5 1.25"
              stroke="#000000"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
        </div>
      </div>
    </div>
  );
}