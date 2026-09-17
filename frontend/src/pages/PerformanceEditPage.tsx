import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { useParams } from 'react-router-dom'
import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogTitle from '@mui/material/DialogTitle'
import Skeleton from '@mui/material/Skeleton'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { ApiError, cancelPerformance, getPerformance, updatePerformance } from '../api/performances'
import type { Performance } from '../api/performances'
import { StatusBadge } from '../components/StatusBadge'
import { fromDatetimeLocalInput, toDatetimeLocalInput } from '../utils/datetime'

type LoadState = { status: 'loading' } | { status: 'error' } | { status: 'success' }

export function PerformanceEditPage() {
  const { id } = useParams<{ id: string }>()
  const [loadState, setLoadState] = useState<LoadState>({ status: 'loading' })
  const [performance, setPerformance] = useState<Performance | null>(null)
  const [title, setTitle] = useState('')
  const [venue, setVenue] = useState('')
  const [startAt, setStartAt] = useState('')
  const [openAt, setOpenAt] = useState('')
  const [closeAt, setCloseAt] = useState('')
  const [saveError, setSaveError] = useState<string | null>(null)
  const [cancelDialogOpen, setCancelDialogOpen] = useState(false)

  function load() {
    if (!id) return
    setLoadState({ status: 'loading' })
    getPerformance(id)
      .then((loaded) => {
        setPerformance(loaded)
        setTitle(loaded.title)
        setVenue(loaded.venue)
        setStartAt(toDatetimeLocalInput(loaded.startAt))
        setOpenAt(toDatetimeLocalInput(loaded.openAt))
        setCloseAt(toDatetimeLocalInput(loaded.closeAt))
        setLoadState({ status: 'success' })
      })
      .catch(() => setLoadState({ status: 'error' }))
  }

  useEffect(load, [id])

  async function save() {
    if (!id) return
    setSaveError(null)
    try {
      const updated = await updatePerformance(id, {
        title,
        venue,
        startAt: fromDatetimeLocalInput(startAt),
        openAt: fromDatetimeLocalInput(openAt),
        closeAt: fromDatetimeLocalInput(closeAt),
      })
      // PUT 응답에는 sections가 없다(PerformanceResponse — 상세 GET에서만 채워짐).
      // 응답으로 통째로 교체하면 화면에서 좌석 구성 요약이 사라지므로 기존 값을 보존한다.
      setPerformance((prev) => (prev ? { ...updated, sections: prev.sections } : updated))
    } catch (error) {
      if (error instanceof ApiError && error.code === 'REGISTRATION_ALREADY_OPEN') {
        setSaveError('이미 오픈된 공연은 기본 정보를 수정할 수 없습니다')
      } else {
        setSaveError('저장하지 못했습니다. 다시 시도해 주세요')
      }
    }
  }

  function handleSave(event: FormEvent) {
    event.preventDefault()
    void save()
  }

  async function confirmCancel() {
    if (!id) return
    try {
      const updated = await cancelPerformance(id)
      // POST cancel 응답에도 sections가 없다 — 저장과 동일한 이유로 기존 값을 보존한다.
      setPerformance((prev) => (prev ? { ...updated, sections: prev.sections } : updated))
      setCancelDialogOpen(false)
    } catch {
      setCancelDialogOpen(false)
      setSaveError('취소하지 못했습니다. 다시 시도해 주세요')
    }
  }

  if (loadState.status === 'loading') {
    return <Skeleton variant="rectangular" height={240} />
  }

  if (loadState.status === 'error' || !performance) {
    return (
      <Alert
        severity="error"
        data-testid="form-error"
        action={
          <Button color="inherit" size="small" onClick={load}>
            다시 시도
          </Button>
        }
      >
        불러오지 못했습니다. 다시 시도해 주세요
      </Alert>
    )
  }

  return (
    <Stack component="form" spacing={2} onSubmit={handleSave}>
      <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography variant="h5">공연 수정</Typography>
        <StatusBadge status={performance.status} />
      </Stack>

      {saveError && <Alert severity="error">{saveError}</Alert>}

      <TextField label="공연명" value={title} onChange={(e) => setTitle(e.target.value)} required />
      <TextField label="장소" value={venue} onChange={(e) => setVenue(e.target.value)} required />
      <TextField
        label="공연일시"
        type="datetime-local"
        value={startAt}
        onChange={(e) => setStartAt(e.target.value)}
        slotProps={{ inputLabel: { shrink: true } }}
        required
      />
      <TextField
        label="오픈"
        type="datetime-local"
        value={openAt}
        onChange={(e) => setOpenAt(e.target.value)}
        slotProps={{ inputLabel: { shrink: true } }}
        required
      />
      <TextField
        label="마감"
        type="datetime-local"
        value={closeAt}
        onChange={(e) => setCloseAt(e.target.value)}
        slotProps={{ inputLabel: { shrink: true } }}
        required
      />

      <Stack data-testid="section-summary" spacing={0.5}>
        <Typography variant="subtitle2">좌석 구성</Typography>
        {performance.sections?.map((section) => (
          <Typography key={`${section.grade}-${section.price}`} variant="body2">
            {section.grade} · {section.price.toLocaleString('ko-KR')}원 · {section.seatCount}석
          </Typography>
        ))}
      </Stack>

      <Button type="submit" variant="contained">
        저장
      </Button>
      <Button color="error" onClick={() => setCancelDialogOpen(true)}>
        공연 취소
      </Button>

      <Dialog open={cancelDialogOpen} onClose={() => setCancelDialogOpen(false)}>
        <DialogTitle>이 공연을 취소하시겠습니까?</DialogTitle>
        <DialogActions>
          <Button onClick={() => setCancelDialogOpen(false)}>취소</Button>
          <Button onClick={() => void confirmCancel()}>확인</Button>
        </DialogActions>
      </Dialog>
    </Stack>
  )
}
