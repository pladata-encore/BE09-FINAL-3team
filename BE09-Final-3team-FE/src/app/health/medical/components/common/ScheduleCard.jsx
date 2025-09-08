"use client";

import React from "react";
import styles from "../../styles/CareManagement.module.css";
import { COLOR_MAP } from "../../../constants/colors";
import { ICON_MAP, SUBTYPE_LABEL_MAP } from "../../../constants";
import { formatTime } from "../../../constants/schedule";
import { CARE_LABELS, VACCINATION_LABELS } from "../../../constants";

export default function ScheduleCard({
  schedule,
  type,
  onEdit,
  onDelete,
  onToggleNotification,
}) {
  const getScheduleIcon = (subType) => {
    return ICON_MAP[subType] || "📅";
  };

  const getScheduleLabel = (subType) => {
    return SUBTYPE_LABEL_MAP[subType] || subType;
  };

  // 확장된 일정의 경우 날짜 정보 표시
  const getDateInfo = () => {
    if (schedule.displayDate && schedule.displayDate !== schedule.startDate) {
      const displayDate = new Date(schedule.displayDate);
      const formattedDate = `${
        displayDate.getMonth() + 1
      }월 ${displayDate.getDate()}일`;
      return formattedDate;
    }
    return null;
  };

  const dateInfo = getDateInfo();

  return (
    <div
      key={schedule.displayKey || schedule.id}
      className={styles.scheduleCard}
    >
      <div className={styles.scheduleInfo}>
        <div
          className={styles.scheduleIcon}
          style={{
            backgroundColor: COLOR_MAP[schedule.subType] || "#e8f5e8",
          }}
        >
          {getScheduleIcon(schedule.subType)}
        </div>
        <div className={styles.scheduleDetails}>
          <h4>{schedule.title || schedule.name}</h4>
          <p>{schedule.frequency || schedule.careFrequency}</p>
          {dateInfo && <p className={styles.scheduleDate}>{dateInfo}</p>}
          <p className={styles.scheduleTime}>
            {formatTime(schedule.scheduleTime)}
          </p>
        </div>
      </div>
      <div className={styles.scheduleActions}>
        <button
          className={styles.actionButton}
          onClick={() => onEdit(schedule.id, type)}
        >
          <img 
            src="/health/note.png" 
            alt={type === "돌봄" ? CARE_LABELS.EDIT : VACCINATION_LABELS.EDIT} 
            width={22} 
            height={22} 
          />
        </button>
        <button
          className={styles.actionButton}
          onClick={() => onDelete(schedule.id, type)}
        >
          <img 
            src="/health/trash.png" 
            alt={type === "돌봄" ? CARE_LABELS.DELETE : VACCINATION_LABELS.DELETE} 
            width={24} 
            height={24} 
          />
        </button>
        <button
          className={styles.actionButton}
          onClick={() => onToggleNotification(schedule.id, type)}
        >
          <img
            src={
              schedule.isNotified
                ? "/health/notifi.png"
                : "/health/notifi2.png"
            }
            alt="알림"
            width={24}
            height={24}
          />
        </button>
      </div>
    </div>
  );
}
