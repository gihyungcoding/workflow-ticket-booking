import { useState } from 'react'
import type { FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import Card from '@mui/material/Card'
import CardContent from '@mui/material/CardContent'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { ApiError, registerPerformance } from '../api/performances'
import { fromDatetimeLocalInput } from '../utils/datetime'

interface SectionForm {
  id: number
  grade: string
  price: string
  rowStart: string
  rowEnd: string
  seatsPerRow: string
}

function emptySection(id: number): SectionForm {
  return { id, grade: '', price: '', rowStart: '', rowEnd: '', seatsPerRow: '' }
}

function seatCountOf(section: SectionForm): number {
  const rowStart = section.rowStart.trim().toUpperCase().charCodeAt(0)
  const rowEnd = section.rowEnd.trim().toUpperCase().charCodeAt(0)
  const seatsPerRow = Number(section.seatsPerRow)
  if (!Number.isFinite(rowStart) || !Number.isFinite(rowEnd) || !Number.isFinite(seatsPerRow)) {
    return 0
  }
  const rowCount = rowEnd - rowStart + 1
  if (rowCount <= 0 || seatsPerRow <= 0) {
    return 0
  }
  return rowCount * seatsPerRow
}

export function PerformanceRegisterPage() {
  const navigate = useNavigate()
  const [title, setTitle] = useState('')
  const [venue, setVenue] = useState('')
  const [startAt, setStartAt] = useState('')
  const [openAt, setOpenAt] = useState('')
  const [closeAt, setCloseAt] = useState('')
  const [sections, setSections] = useState<SectionForm[]>([emptySection(0)])
  const [nextSectionId, setNextSectionId] = useState(1)
  const [timeFieldsError, setTimeFieldsError] = useState<string | null>(null)
  const [sectionErrors, setSectionErrors] = useState<Record<number, string>>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const seatCounts = sections.map(seatCountOf)
  const totalSeats = seatCounts.reduce((sum, count) => sum + count, 0)

  function addSection() {
    setSections((prev) => [...prev, emptySection(nextSectionId)])
    setNextSectionId((id) => id + 1)
  }

  function updateSection(index: number, field: keyof Omit<SectionForm, 'id'>, value: string) {
    setSections((prev) => prev.map((section, i) => (i === index ? { ...section, [field]: value } : section)))
  }

  async function submit() {
    setTimeFieldsError(null)
    setSectionErrors({})
    setFormError(null)
    setSubmitting(true)
    try {
      const performance = await registerPerformance({
        title,
        venue,
        startAt: fromDatetimeLocalInput(startAt),
        openAt: fromDatetimeLocalInput(openAt),
        closeAt: fromDatetimeLocalInput(closeAt),
        sections: sections.map((section) => ({
          grade: section.grade,
          price: Number(section.price),
          rowStart: section.rowStart,
          rowEnd: section.rowEnd,
          seatsPerRow: Number(section.seatsPerRow),
        })),
      })
      navigate(`/performances/${performance.id}`)
    } catch (error) {
      if (error instanceof ApiError && error.code === 'INVALID_TIME_ORDER') {
        setTimeFieldsError('예매 오픈·마감·공연 일시 순서가 올바르지 않습니다')
      } else if (error instanceof ApiError && error.code === 'SEAT_LIMIT_EXCEEDED') {
        setSectionErrors({ [sections.length - 1]: '좌석 총수는 5,000석을 넘을 수 없습니다' })
      } else {
        setFormError('등록하지 못했습니다. 다시 시도해 주세요')
      }
    } finally {
      setSubmitting(false)
    }
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    void submit()
  }

  return (
    <Stack component="form" spacing={2} onSubmit={handleSubmit}>
      <Typography variant="h5">공연 등록</Typography>

      {formError && (
        <Alert
          severity="error"
          data-testid="form-error"
          action={
            <Button color="inherit" size="small" onClick={() => void submit()}>
              다시 시도
            </Button>
          }
        >
          {formError}
        </Alert>
      )}

      <TextField label="공연명" value={title} onChange={(e) => setTitle(e.target.value)} />
      <TextField label="장소" value={venue} onChange={(e) => setVenue(e.target.value)} />
      <TextField
        label="공연일시"
        type="datetime-local"
        value={startAt}
        onChange={(e) => setStartAt(e.target.value)}
        slotProps={{ inputLabel: { shrink: true } }}
      />
      <TextField
        label="오픈"
        type="datetime-local"
        value={openAt}
        onChange={(e) => setOpenAt(e.target.value)}
        slotProps={{ inputLabel: { shrink: true } }}
      />
      <TextField
        label="마감"
        type="datetime-local"
        value={closeAt}
        onChange={(e) => setCloseAt(e.target.value)}
        slotProps={{ inputLabel: { shrink: true } }}
      />
      {timeFieldsError && (
        <Alert severity="error" data-testid="time-fields-error">
          {timeFieldsError}
        </Alert>
      )}

      <Stack spacing={2}>
        {sections.map((section, index) => (
          <Card key={section.id} variant="outlined" data-testid={`section-card-${index}`}>
            <CardContent>
              <Stack spacing={1}>
                <TextField
                  label="등급"
                  value={section.grade}
                  onChange={(e) => updateSection(index, 'grade', e.target.value)}
                />
                <TextField
                  label="가격"
                  type="number"
                  value={section.price}
                  onChange={(e) => updateSection(index, 'price', e.target.value)}
                />
                <TextField
                  label="시작 행"
                  value={section.rowStart}
                  onChange={(e) => updateSection(index, 'rowStart', e.target.value)}
                />
                <TextField
                  label="종료 행"
                  value={section.rowEnd}
                  onChange={(e) => updateSection(index, 'rowEnd', e.target.value)}
                />
                <TextField
                  label="행당 좌석수"
                  type="number"
                  value={section.seatsPerRow}
                  onChange={(e) => updateSection(index, 'seatsPerRow', e.target.value)}
                />
                {sectionErrors[index] && <Alert severity="error">{sectionErrors[index]}</Alert>}
              </Stack>
            </CardContent>
          </Card>
        ))}
      </Stack>

      <Button onClick={addSection}>구역 추가</Button>

      <Stack data-testid="seat-preview" spacing={0.5}>
        {sections.map((section, index) => (
          <Typography key={section.id} variant="body2" color="textSecondary">
            {section.grade || '(등급 미입력)'} {seatCounts[index]}석
          </Typography>
        ))}
        <Typography variant="body2">총 {totalSeats}석</Typography>
      </Stack>

      <Button type="submit" variant="contained" disabled={submitting}>
        등록
      </Button>
    </Stack>
  )
}
