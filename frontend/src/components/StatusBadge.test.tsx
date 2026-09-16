import { render, screen } from '@testing-library/react'
import { StatusBadge } from './StatusBadge'

describe('StatusBadge', () => {
  test('SC-04 (happy) 상태 문구가 의미 매핑대로 표시된다', () => {
    // Given
    // StatusBadge 에 UPCOMING·OPEN·SOLD_OUT·CLOSED·CANCELLED 각각을 준다

    // When
    // 다섯 개를 각각 렌더한다
    render(
      <>
        <StatusBadge status="UPCOMING" />
        <StatusBadge status="OPEN" />
        <StatusBadge status="SOLD_OUT" />
        <StatusBadge status="CLOSED" />
        <StatusBadge status="CANCELLED" />
      </>,
    )

    // Then
    expect(screen.getByText('예매예정')).toBeInTheDocument()
    expect(screen.getByText('예매가능')).toBeInTheDocument()
    expect(screen.getByText('매진')).toBeInTheDocument()
    expect(screen.getByText('예매마감')).toBeInTheDocument()
    expect(screen.getByText('공연취소')).toBeInTheDocument()
  })
})
